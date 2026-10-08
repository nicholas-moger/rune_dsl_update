from __future__ import annotations
from dataclasses import dataclass
from enum import Enum
from typing import List, Optional, Union
from decimal import Decimal
from datetime import date, datetime, time

@dataclass
class CamelCaseNames:
    day_count_fraction: Decimal = None
    notional_usd_value: Decimal = None
    payer_receiver: Optional[str] = None
