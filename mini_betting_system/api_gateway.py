import time
from typing import Dict

from data_model import CryptoAsset, BetDirection, Bet
from services import BettingService, UserService, OddsService


# ---- API GATEWAY ----
class APIGateway:
    """Simulates an API Gateway"""

    def __init__(self, betting_service: BettingService, user_service: UserService, odds_service: OddsService):
        self._betting_service = betting_service
        self._user_service = user_service
        self._odds_service = odds_service
        self._rate_limits: Dict[str, int] = {}

    async def handle_request(self, endpoint: str, method: str, data: dict) -> Dict[str, any]:
        """Handle incoming API requests"""
        # Rate limiting
        client_id = data.get("client_id", "anonymous")
        if not self._check_rate_limit(client_id):
            return {"error": "Rate limit exceeded", "status_code": 429}

        # Route to appropriate handler
        if endpoint == "/api/bet" and method == "POST":
            return await self._handle_place_bet(data)
        elif endpoint == "/api/bets" and method == "GET":
            return await self._handle_get_bets(data)
        elif endpoint == "/api/odds" and method == "GET":
            return await self._handle_get_odds(data)
        elif endpoint == "/api/user" and method == "GET":
            return await self._handle_get_user(data)
        else:
            return {"error": "Not found", "status_code": 404}

    def _check_rate_limit(self, client_id: str) -> bool:
        """Simple rate limiting"""
        current_time = int(time.time())
        minute_bucket = current_time // 60

        rate_key = f"{client_id}:{minute_bucket}"
        current_count = self._rate_limits.get(rate_key, 0)

        if current_count >= 100:  # 100 requests per minute
            return False

        self._rate_limits[rate_key] = current_count + 1
        return True

    async def _handle_place_bet(self, data: dict) -> Dict[str, any]:
        """Handle bet placement request"""
        required_fields = ["user_id", "asset", "direction", "amount"]
        missing_fields = [field for field in required_fields if field not in data]

        if missing_fields:
            return {
                "error": f"Missing required fields: {', '.join(missing_fields)}",
                "status_code": 400
            }

        # Validate enum values
        try:
            asset = CryptoAsset(data["asset"])
            direction = BetDirection(data["direction"])
        except ValueError:
            return {"error": "Invalid asset or direction", "status_code": 400}

        # Place bet
        result = await self._betting_service.place_bet(
            user_id=data["user_id"],
            asset=asset,
            direction=direction,
            amount=float(data["amount"])
        )

        if result.get("success", False):
            return {"data": result, "status_code": 200}
        else:
            return {"error": result.get("error", "Unknown error"), "status_code": 400}

    async def _handle_get_bets(self, data: dict) -> Dict[str, any]:
        """Handle get user bets request"""
        user_id = data.get("user_id")
        if not user_id:
            return {"error": "Missing user_id", "status_code": 400}

        bets = await self._betting_service.get_user_bets(user_id)
        return {
            "data": [self._serialize_bet(bet) for bet in bets],
            "status_code": 200
        }

    async def _handle_get_odds(self, data: dict) -> Dict[str, any]:
        """Handle get odds request"""
        asset = data.get("asset")
        direction = data.get("direction")

        if not asset or not direction:
            return {"error": "Missing asset or direction", "status_code": 400}

        try:
            asset_enum = CryptoAsset(asset)
            direction_enum = BetDirection(direction)
        except ValueError:
            return {"error": "Invalid asset or direction", "status_code": 400}

        odds = await self._odds_service.get_odds(asset_enum, direction_enum)
        price = await self._odds_service.get_latest_price(asset_enum)

        return {
            "data": {
                "asset": asset,
                "direction": direction,
                "odds": odds,
                "current_price": price
            },
            "status_code": 200
        }

    async def _handle_get_user(self, data: dict) -> Dict[str, any]:
        """Handle get user request"""
        user_id = data.get("user_id")
        if not user_id:
            return {"error": "Missing user_id", "status_code": 400}

        user = await self._user_service.get_user(user_id)
        if not user:
            return {"error": "User not found", "status_code": 404}

        return {
            "data": {
                "user_id": user.user_id,
                "username": user.username,
                "balance": user.balance
            },
            "status_code": 200
        }

    def _serialize_bet(self, bet: Bet) -> Dict[str, any]:
        """Convert bet object to dict"""
        return {
            "bet_id": bet.bet_id,
            "user_id": bet.user_id,
            "asset": bet.asset,
            "direction": bet.direction,
            "amount": bet.amount,
            "odds": bet.odds,
            "created_at": bet.created_at.isoformat(),
            "status": bet.status,
            "settled_at": bet.settled_at.isoformat() if bet.settled_at else None
        }
