import asyncio
from datetime import datetime
from typing import Dict, List, Optional

from data_model import CryptoPrice, User, Bet, CryptoAsset, Event, BetStatus


# ---- DATABASE LAYER ----
class Database:
    """Simulates a database with separate read/write paths"""

    def __init__(self):
        # Main storage
        self._users: Dict[str, User] = {}
        self._bets: Dict[str, Bet] = {}
        self._prices: Dict[str, List[CryptoPrice]] = {
            CryptoAsset.BTC: [],
            CryptoAsset.ETH: []
        }
        self._events: List[Event] = []

        # Read replica (simulated with delayed updates)
        self._read_users: Dict[str, User] = {}
        self._read_bets: Dict[str, Bet] = {}

        # Initialize with sample data
        self._initialize_sample_data()

    def _initialize_sample_data(self):
        # Create sample users
        for i in range(1, 5):
            user_id = f"user{i}"
            user = User(
                user_id=user_id,
                username=f"User {i}",
                balance=1000.0,
                last_login=datetime.now()
            )
            self._users[user_id] = user
            self._read_users[user_id] = user

        # Initialize crypto prices
        current_prices = {
            CryptoAsset.BTC: 45000.0,
            CryptoAsset.ETH: 3200.0
        }

        for asset in [CryptoAsset.BTC, CryptoAsset.ETH]:
            self._prices[asset].append(
                CryptoPrice(
                    asset=asset,
                    price=current_prices[asset],
                    timestamp=datetime.now()
                )
            )

    # Write operations (primary DB)
    def write_user(self, user: User) -> None:
        """Write to primary storage"""
        self._users[user.user_id] = user
        # Simulate eventual consistency
        asyncio.create_task(self._update_read_replica_user(user))

    def write_bet(self, bet: Bet) -> None:
        """Write to primary storage"""
        self._bets[bet.bet_id] = bet
        # Simulate eventual consistency
        asyncio.create_task(self._update_read_replica_bet(bet))

    def write_price(self, price: CryptoPrice) -> None:
        """Record new price"""
        self._prices[price.asset].append(price)

    def write_event(self, event: Event) -> None:
        """Record new event"""
        self._events.append(event)

    # Read operations (read replicas)
    def read_user(self, user_id: str) -> Optional[User]:
        """Read from replica"""
        return self._read_users.get(user_id)

    def read_bet(self, bet_id: str) -> Optional[Bet]:
        """Read from replica"""
        return self._read_bets.get(bet_id)

    def read_bets_by_user(self, user_id: str) -> List[Bet]:
        """Read from replica"""
        return [bet for bet in self._read_bets.values() if bet.user_id == user_id]

    def read_latest_price(self, asset: CryptoAsset) -> Optional[CryptoPrice]:
        """Get latest price"""
        if not self._prices[asset]:
            return None
        return self._prices[asset][-1]

    def read_pending_events(self) -> List[Event]:
        """Get unprocessed events"""
        return [e for e in self._events if not e.processed]

    # Direct DB access (for admin/system use)
    def update_bet_status(self, bet_id: str, status: BetStatus) -> None:
        """Direct update for admin operations"""
        if bet_id in self._bets:
            self._bets[bet_id].status = status
            if status in [BetStatus.SETTLED_WIN, BetStatus.SETTLED_LOSS]:
                self._bets[bet_id].settled_at = datetime.now()
            asyncio.create_task(self._update_read_replica_bet(self._bets[bet_id]))

    def mark_event_processed(self, event_id: str) -> None:
        """Mark event as processed"""
        for event in self._events:
            if event.event_id == event_id:
                event.processed = True
                break

    # Simulated eventual consistency
    async def _update_read_replica_user(self, user: User) -> None:
        """Simulate read replica lag"""
        await asyncio.sleep(0.05)  # 50ms replica lag
        self._read_users[user.user_id] = user

    async def _update_read_replica_bet(self, bet: Bet) -> None:
        """Simulate read replica lag"""
        await asyncio.sleep(0.05)  # 50ms replica lag
        self._read_bets[bet.bet_id] = bet
