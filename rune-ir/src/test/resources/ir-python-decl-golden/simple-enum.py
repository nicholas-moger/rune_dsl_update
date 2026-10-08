from __future__ import annotations
from dataclasses import dataclass
from enum import Enum
from typing import List, Optional, Union
from decimal import Decimal
from datetime import date, datetime, time

class DirectionEnum(Enum):
    Up = "Up"
    Down = "Down"
