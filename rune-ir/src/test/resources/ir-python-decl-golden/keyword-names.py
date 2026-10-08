from __future__ import annotations
from dataclasses import dataclass
from enum import Enum
from typing import List, Optional, Union
from decimal import Decimal
from datetime import date, datetime, time

@dataclass
class KeywordFields:
    global_: Optional[Decimal] = None
    return_: Optional[Decimal] = None

class CompoundingMethodEnum(Enum):
    None_ = "None"
    Flat = "Flat"
