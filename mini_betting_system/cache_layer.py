import time

from typing import Dict, List, Optional, Tuple, Union


# ---- CACHE LAYER ----
class Cache:
    """Simulates a Redis cache"""

    def __init__(self):
        self._data: Dict[str, Tuple[any, float]] = {}
        self._default_ttl = 60  # seconds

    async def get(self, key: str) -> Optional[any]:
        """Get value from cache if not expired"""
        if key not in self._data:
            return None

        value, expiry = self._data[key]
        if time.time() > expiry:
            del self._data[key]
            return None

        return value

    async def set(self, key: str, value: any, ttl: Optional[float] = None) -> None:
        """Set value with expiration"""
        expiry = time.time() + (ttl or self._default_ttl)
        self._data[key] = (value, expiry)

    async def delete(self, key: str) -> None:
        """Remove key from cache"""
        if key in self._data:
            del self._data[key]

    async def flush(self) -> None:
        """Clear all cache"""
        self._data.clear()
