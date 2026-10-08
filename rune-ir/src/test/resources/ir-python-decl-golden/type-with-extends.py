from __future__ import annotations
from dataclasses import dataclass
from enum import Enum
from typing import List, Optional, Union
from decimal import Decimal
from datetime import date, datetime, time

@dataclass
class Base:
    id: str = None

@dataclass
class Child(Base):
    extra: Optional[int] = None
