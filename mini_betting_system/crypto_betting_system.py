# Minimalistic Crypto Betting System
# This demonstrates key architectural patterns
# 1. Event-driven communication
# 2. Microservices architecture
# 3. Read/write separation
# 4. Fast path/slow path separation
# 5. Caching for performance

import json
import asyncio

from api_gateway import APIGateway
from cache_layer import Cache
from db_layer import Database
from event_bus import EventBus
from services import UserService, OddsService, BettingService, SettlementService, MarketDataService


# ---- SYSTEM SETUP ----
async def setup_system():
    """Initialize and connect all components"""
    # Core infrastructure
    db = Database()
    cache = Cache()
    event_bus = EventBus(db)

    # Services
    user_service = UserService(db, cache)
    odds_service = OddsService(db, cache, event_bus)
    betting_service = BettingService(db, cache, event_bus, user_service, odds_service)
    settlement_service = SettlementService(db, event_bus, odds_service)
    market_data_service = MarketDataService(db, event_bus)

    # API Gateway
    api_gateway = APIGateway(betting_service, user_service, odds_service)

    # Start market data simulation
    await market_data_service.start()

    return {
        "db": db,
        "cache": cache,
        "event_bus": event_bus,
        "user_service": user_service,
        "odds_service": odds_service,
        "betting_service": betting_service,
        "settlement_service": settlement_service,
        "market_data_service": market_data_service,
        "api_gateway": api_gateway
    }


# ---- DEMO ----
async def run_demo():
    """Run a demonstration of the system"""
    print("Setting up crypto betting system...")
    system = await setup_system()
    api = system["api_gateway"]

    print("\n=== DEMO: Crypto Betting System ===")

    # Get user details
    print("\n1. Getting user details...")
    user_response = await api.handle_request(
        "/api/user",
        "GET",
        {"user_id": "user1", "client_id": "demo"}
    )
    print(f"User details: {json.dumps(user_response, indent=2)}")

    # Get current odds
    print("\n2. Getting current odds...")
    odds_response = await api.handle_request(
        "/api/odds",
        "GET",
        {"asset": "BTC", "direction": "UP", "client_id": "demo"}
    )
    print(f"Current odds: {json.dumps(odds_response, indent=2)}")

    # Place a bet
    print("\n3. Placing a bet...")
    bet_response = await api.handle_request(
        "/api/bet",
        "POST",
        {
            "user_id": "user1",
            "asset": "BTC",
            "direction": "UP",
            "amount": 100,
            "client_id": "demo"
        }
    )
    print(f"Bet placement result: {json.dumps(bet_response, indent=2)}")
    bet_id = bet_response.get("data", {}).get("bet_id")

    # Wait for async processing
    print("\nWaiting for async processing...")
    await asyncio.sleep(1)

    # Get user bets
    print("\n4. Getting user bets...")
    bets_response = await api.handle_request(
        "/api/bets",
        "GET",
        {"user_id": "user1", "client_id": "demo"}
    )
    print(f"User bets: {json.dumps(bets_response, indent=2)}")

    # Wait for price update and potential settlement
    print("\nWaiting for price updates and settlement...")
    await asyncio.sleep(6)

    # Get updated bets
    print("\n5. Getting updated bets after settlement...")
    bets_response = await api.handle_request(
        "/api/bets",
        "GET",
        {"user_id": "user1", "client_id": "demo"}
    )
    print(f"Updated bets: {json.dumps(bets_response, indent=2)}")

    # Final user balance
    print("\n6. Getting final user balance...")
    user_response = await api.handle_request(
        "/api/user",
        "GET",
        {"user_id": "user1", "client_id": "demo"}
    )
    print(f"Final user details: {json.dumps(user_response, indent=2)}")

    print("\nDemo complete!")

    # Shutdown - properly await the stop method
    await system["market_data_service"].stop()


if __name__ == "__main__":
    # Run the demo
    asyncio.run(run_demo())