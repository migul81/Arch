import uuid
import asyncio
from datetime import datetime
from typing import Dict, List

from data_model import Event
from db_layer import Database


# ---- EVENT BUS ----
class EventBus:
    """Simulates a message broker like Kafka/RabbitMQ"""

    def __init__(self, db: Database):
        self._subscribers: Dict[str, List[callable]] = {}
        self._db = db

    def publish(self, event_type: str, payload: dict) -> str:
        """Publish an event to the bus"""
        event_id = str(uuid.uuid4())
        event = Event(
            event_id=event_id,
            event_type=event_type,
            timestamp=datetime.now(),
            payload=payload
        )

        # Persist event first (event sourcing pattern)
        self._db.write_event(event)

        # Then process asynchronously
        asyncio.create_task(self._process_event(event))
        return event_id

    def subscribe(self, event_type: str, callback: callable) -> None:
        """Subscribe to an event type"""
        if event_type not in self._subscribers:
            self._subscribers[event_type] = []
        self._subscribers[event_type].append(callback)

    async def _process_event(self, event: Event) -> None:
        """Process event by notifying all subscribers"""
        if event.event_type in self._subscribers:
            for callback in self._subscribers[event.event_type]:
                try:
                    await callback(event.payload)
                except Exception as e:
                    print(f"Error processing event {event.event_id}: {e}")

        # Mark as processed
        self._db.mark_event_processed(event.event_id)

    async def replay_events(self) -> None:
        """Replay unprocessed events (recovery pattern)"""
        for event in self._db.read_pending_events():
            await self._process_event(event)
