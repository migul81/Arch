import uuid
import asyncio
import random
from datetime import datetime
from typing import Dict, List, Optional

from cache_layer import Cache
from data_model import User, CryptoAsset, BetDirection, BetStatus, Bet, CryptoPrice
from db_layer import Database
from event_bus import EventBus


# ---- SERVICES ----
class UserService:
    """Handles user-related operations"""

    def __init__(self, db: Database, cache: Cache):
        self._db = db
        self._cache = cache

    async def get_user(self, user_id: str) -> Optional[User]:
        """Get user with cache first strategy"""
        # Try cache first (fast path)
        cache_key = f"user:{user_id}"
        cached_user = await self._cache.get(cache_key)
        if cached_user:
            return cached_user

        # Cache miss, get from DB (slow path)
        user = self._db.read_user(user_id)
        if user:
            # Update cache for next time
            await self._cache.set(cache_key, user)
        return user

    async def update_balance(self, user_id: str, delta: float) -> bool:
        """Update user balance (write operation)"""
        user = self._db.read_user(user_id)
        if not user:
            return False

        new_balance = user.balance + delta
        if new_balance < 0:
            return False

        user.balance = new_balance
        self._db.write_user(user)

        # Invalidate cache
        await self._cache.delete(f"user:{user_id}")
        return True


class OddsService:
    """Handles odds calculations and price updates"""

    def __init__(self, db: Database, cache: Cache, event_bus: EventBus):
        self._db = db
        self._cache = cache
        self._event_bus = event_bus

        # Subscribe to price updates
        self._event_bus.subscribe("price_updated", self._handle_price_update)

    async def get_latest_price(self, asset: CryptoAsset) -> Optional[float]:
        """Get latest price with caching"""
        cache_key = f"price:{asset}"
        cached_price = await self._cache.get(cache_key)
        if cached_price:
            return cached_price

        price_obj = self._db.read_latest_price(asset)
        if not price_obj:
            return None

        await self._cache.set(cache_key, price_obj.price, ttl=5)  # Short TTL for prices
        return price_obj.price

    async def get_odds(self, asset: CryptoAsset, direction: BetDirection) -> float:
        """Calculate betting odds based on market volatility"""
        # Simple odds calculation
        base_odds = 1.95  # Close to even odds

        # Adjust based on recent volatility (simplified)
        volatility_adjustment = random.uniform(0.9, 1.1)
        odds = base_odds * volatility_adjustment

        # Cap odds at reasonable values
        return min(max(odds, 1.1), 3.0)

    async def _handle_price_update(self, payload: dict) -> None:
        """Handle price update events"""
        asset = payload.get("asset")
        new_price = payload.get("price")

        # Invalidate price cache
        await self._cache.delete(f"price:{asset}")

        # In a real system, we'd recalculate odds for all affected bets


class BettingService:
    """Core betting service"""

    def __init__(self,
                 db: Database,
                 cache: Cache,
                 event_bus: EventBus,
                 user_service: UserService,
                 odds_service: OddsService):
        self._db = db
        self._cache = cache
        self._event_bus = event_bus
        self._user_service = user_service
        self._odds_service = odds_service

        # Subscribe to events
        self._event_bus.subscribe("bet_settled", self._handle_bet_settled)

    async def place_bet(self,
                        user_id: str,
                        asset: CryptoAsset,
                        direction: BetDirection,
                        amount: float) -> Dict[str, any]:
        """Place a new bet (fast path validation)"""
        # Fast validations first
        user = await self._user_service.get_user(user_id)
        if not user:
            return {"success": False, "error": "User not found"}

        if user.balance < amount:
            return {"success": False, "error": "Insufficient balance"}

        if amount <= 0:
            return {"success": False, "error": "Invalid bet amount"}

        # Get current odds
        odds = await self._odds_service.get_odds(asset, direction)

        # Create bet in PENDING status
        bet_id = str(uuid.uuid4())
        bet = Bet(
            bet_id=bet_id,
            user_id=user_id,
            asset=asset,
            direction=direction,
            amount=amount,
            odds=odds,
            created_at=datetime.now(),
            status=BetStatus.PENDING
        )

        # Reserve funds (optimistic lock)
        balance_updated = await self._user_service.update_balance(user_id, -amount)
        if not balance_updated:
            return {"success": False, "error": "Failed to reserve funds"}

        # Save bet to database
        self._db.write_bet(bet)

        # Publish event for async processing
        self._event_bus.publish("bet_placed", {
            "bet_id": bet_id,
            "user_id": user_id,
            "asset": asset,
            "direction": direction,
            "amount": amount,
            "odds": odds
        })

        # Return fast response
        return {
            "success": True,
            "bet_id": bet_id,
            "message": "Bet accepted, processing payment"
        }

    async def get_bet(self, bet_id: str) -> Optional[Bet]:
        """Get bet details"""
        return self._db.read_bet(bet_id)

    async def get_user_bets(self, user_id: str) -> List[Bet]:
        """Get all bets for a user"""
        return self._db.read_bets_by_user(user_id)

    async def _handle_bet_settled(self, payload: dict) -> None:
        """Handle bet settlement events"""
        bet_id = payload.get("bet_id")
        outcome = payload.get("outcome")

        if not bet_id or not outcome:
            return

        bet = self._db.read_bet(bet_id)
        if not bet:
            return

        status = BetStatus.SETTLED_WIN if outcome == "win" else BetStatus.SETTLED_LOSS
        self._db.update_bet_status(bet_id, status)

        # If win, credit winnings
        if outcome == "win":
            winnings = bet.amount * bet.odds
            await self._user_service.update_balance(bet.user_id, winnings)


class SettlementService:
    """Handles asynchronous bet settlement"""

    def __init__(self,
                 db: Database,
                 event_bus: EventBus,
                 odds_service: OddsService):
        self._db = db
        self._event_bus = event_bus
        self._odds_service = odds_service

        # Subscribe to events
        self._event_bus.subscribe("bet_placed", self._handle_bet_placed)
        self._event_bus.subscribe("price_updated", self._handle_price_update)

    async def _handle_bet_placed(self, payload: dict) -> None:
        """Accept the bet (async processing)"""
        bet_id = payload.get("bet_id")
        if not bet_id:
            return

        bet = self._db.read_bet(bet_id)
        if not bet or bet.status != BetStatus.PENDING:
            return

        # Accept the bet (async)
        self._db.update_bet_status(bet_id, BetStatus.ACCEPTED)

    async def _handle_price_update(self, payload: dict) -> None:
        """Check if any bets should be settled based on new price"""
        asset = payload.get("asset")
        price = payload.get("price")

        if not asset or not price:
            return

        # In a real system, we'd query for all relevant active bets
        # For simplicity, we're simulating settlement with random outcomes

        # Simulate settlement (demo only - real logic would compare prices)
        for bet in self._db.read_bets_by_user("demo"):
            if bet.asset == asset and bet.status == BetStatus.ACCEPTED:
                # Random outcome for demo
                outcome = "win" if random.random() > 0.5 else "loss"

                # Publish settlement event
                self._event_bus.publish("bet_settled", {
                    "bet_id": bet.bet_id,
                    "outcome": outcome,
                    "settlement_price": price
                })


class MarketDataService:
    """Simulates market data updates"""

    def __init__(self, db: Database, event_bus: EventBus):
        self._db = db
        self._event_bus = event_bus
        self._running = False
        self._current_prices = {
            CryptoAsset.BTC: 45000.0,
            CryptoAsset.ETH: 3200.0
        }

    async def start(self) -> None:
        """Start producing market data"""
        self._running = True
        asyncio.create_task(self._generate_price_updates())

    async def stop(self) -> None:
        """Stop producing market data"""
        self._running = False

    async def _generate_price_updates(self) -> None:
        """Simulate market data updates"""
        while self._running:
            # Update each asset price
            for asset in [CryptoAsset.BTC, CryptoAsset.ETH]:
                # Simulate price movement
                change_pct = random.uniform(-0.01, 0.01)  # 1% max movement
                self._current_prices[asset] *= (1 + change_pct)

                # Create price update
                price = CryptoPrice(
                    asset=asset,
                    price=self._current_prices[asset],
                    timestamp=datetime.now()
                )

                # Save to DB
                self._db.write_price(price)

                # Publish event
                self._event_bus.publish("price_updated", {
                    "asset": asset,
                    "price": price.price,
                    "timestamp": price.timestamp.isoformat()
                })

            # Wait for next update
            await asyncio.sleep(5)  # Update every 5 seconds
