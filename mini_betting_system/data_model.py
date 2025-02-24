from enum import Enum
from dataclasses import dataclass
from datetime import datetime
from typing import Optional


# ---- MODELS ----
class CryptoAsset(str, Enum):
    BTC = "BTC"
    ETH = "ETH"


class BetDirection(str, Enum):
    UP = "UP"
    DOWN = "DOWN"


class BetStatus(str, Enum):
    PENDING = "PENDING"
    ACCEPTED = "ACCEPTED"
    SETTLED_WIN = "SETTLED_WIN"
    SETTLED_LOSS = "SETTLED_LOSS"
    REJECTED = "REJECTED"
    FAILED = "FAILED"


@dataclass
class User:
    user_id: str
    username: str
    balance: float
    last_login: datetime


@dataclass
class Bet:
    bet_id: str
    user_id: str
    asset: CryptoAsset
    direction: BetDirection
    amount: float
    odds: float
    created_at: datetime
    status: BetStatus
    settled_at: Optional[datetime] = None


@dataclass
class CryptoPrice:
    asset: CryptoAsset
    price: float
    timestamp: datetime


# ---- EVENTS ----
@dataclass
class Event:
    event_id: str
    event_type: str
    timestamp: datetime
    payload: dict
    processed: bool = False

