"""
Q1-Q4 analytics over a parser-independent Rune model index.

This module deliberately has NO dependency on ANTLR or on the generated parser:
it operates on plain dataclasses that `analytics.py` fills in from the parse
trees. That separation makes the query semantics unit-testable on hand-built
fixtures (see `--selftest`) and makes any cross-lane disagreement in
CONTRACTS.md section 4 attributable to EXTRACTION rather than to QUERY logic.

Query semantics are fixed by CONTRACTS.md section 5 and must match the
legacy-emf and plus-jvm lanes exactly:

  INDEX  declaration name -> (kind, namespace, file)
  Q1     counts by kind: data types, enums, functions, reporting rules
  Q2     data types carrying ANY [metadata ...] annotation, on the type
         itself or on any of its attributes
  Q3     distinct data types having an attribute whose declared type simple
         name is exactly the target (default "Party")
  Q4     deepest `extends` chain among data types, counted in EDGES (root = 0)

Only NON-BUILTIN declarations are counted. Builtins are the declarations whose
namespace is `com.rosetta` or a descendant of it (CONTRACTS.md section 1).
"""

from __future__ import annotations

import sys
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Sequence, Tuple

# --------------------------------------------------------------------------
# Namespace classification
# --------------------------------------------------------------------------

BUILTIN_NAMESPACE_ROOT = "com.rosetta"


def is_builtin_namespace(namespace: str) -> bool:
    """True for the built-in model namespaces (`com.rosetta`, `com.rosetta.*`).

    The dot-guard matters: a hypothetical `com.rosettafoo` is NOT a builtin.
    """
    if not namespace:
        return False
    return (namespace == BUILTIN_NAMESPACE_ROOT
            or namespace.startswith(BUILTIN_NAMESPACE_ROOT + "."))


# --------------------------------------------------------------------------
# Declaration kinds (one per named grammar production we index)
# --------------------------------------------------------------------------

KIND_TYPE = "type"                    # RosettaParser: dataType
KIND_CHOICE = "choice"                # RosettaParser: choice
KIND_ENUM = "enum"                    # RosettaParser: enumeration
KIND_FUNC = "func"                    # RosettaParser: function
KIND_RULE = "rule"                    # RosettaParser: rosettaRule (reporting + eligibility)
KIND_TYPE_ALIAS = "typeAlias"         # RosettaParser: rosettaTypeAlias
KIND_ANNOTATION = "annotation"        # RosettaParser: annotationDecl
KIND_BASIC_TYPE = "basicType"         # RosettaParser: rosettaBasicType
KIND_RECORD_TYPE = "recordType"       # RosettaParser: rosettaRecordType
KIND_META_TYPE = "metaType"           # RosettaParser: rosettaMetaType
KIND_LIBRARY_FUNCTION = "libraryFunction"   # RosettaParser: rosettaLibraryFunction
KIND_SYNONYM_SOURCE = "synonymSource"       # rosettaSynonymSource / rosettaExternalSynonymSource
KIND_RULE_SOURCE = "ruleSource"             # RosettaParser: rosettaExternalRuleSource
KIND_REPORT = "report"                # RosettaParser: rosettaReport (unnamed)
KIND_BODY = "body"                    # RosettaParser: rosettaBody
KIND_CORPUS = "corpus"                # RosettaParser: rosettaCorpus
KIND_SEGMENT = "segment"              # RosettaParser: rosettaSegment

# The annotation whose presence Q2 tests for. In the built-in model this is
# `annotation metadata:` with qualifiers id/key/reference/scheme/template/
# location/address (demo/corpus/builtins/annotations.rosetta).
METADATA_ANNOTATION = "metadata"


# --------------------------------------------------------------------------
# Model records
# --------------------------------------------------------------------------

@dataclass(slots=True)
class Attr:
    """One `attribute` production: `name TypeCall (card)? [annotations]*`."""
    name: str
    type_qname: str = ""          # declared type exactly as written ("a.b.Party" or "Party")
    type_simple: str = ""         # last segment of type_qname ("Party")
    annotations: Tuple[str, ...] = ()          # annotationRef head names, e.g. ("metadata",)
    annotation_quals: Tuple[str, ...] = ()     # annotationRef qualifiers, e.g. ("scheme",)

    def has_metadata(self) -> bool:
        return METADATA_ANNOTATION in self.annotations


@dataclass(slots=True)
class Decl:
    """One top-level declaration, plus everything the queries need about it."""
    name: str
    kind: str
    namespace: str
    file: str
    extends_qname: Optional[str] = None   # as written; None when no `extends`
    extends_simple: Optional[str] = None  # last segment of extends_qname
    annotations: Tuple[str, ...] = ()     # declaration-level annotationRef head names
    annotation_quals: Tuple[str, ...] = ()
    attributes: List[Attr] = field(default_factory=list)
    line: int = 0
    eligibility: bool = False             # KIND_RULE only: an `eligibility rule` (Q1 excludes these)

    @property
    def builtin(self) -> bool:
        return is_builtin_namespace(self.namespace)

    @property
    def qname(self) -> str:
        return f"{self.namespace}.{self.name}" if self.namespace else self.name

    def has_metadata(self) -> bool:
        """Q2 predicate: [metadata ...] on the declaration or on any attribute."""
        if METADATA_ANNOTATION in self.annotations:
            return True
        return any(a.has_metadata() for a in self.attributes)


@dataclass(slots=True)
class ModelIndex:
    """The CONTRACTS section 5 INDEX plus the walk statistics."""
    decls: List[Decl] = field(default_factory=list)
    # name -> (kind, namespace, file); first declaration of a name wins.
    symbols: Dict[str, Tuple[str, str, str]] = field(default_factory=dict)
    name_collisions: int = 0
    files: int = 0
    bytes_read: int = 0
    parse_errors: int = 0
    error_files: List[str] = field(default_factory=list)
    unknown_elements: int = 0
    sll_bailouts: int = 0
    nodes_visited: int = 0

    # -- index construction -------------------------------------------------

    def add(self, decl: Decl) -> None:
        self.decls.append(decl)
        if decl.name:
            if decl.name in self.symbols:
                self.name_collisions += 1
            else:
                self.symbols[decl.name] = (decl.kind, decl.namespace, decl.file)

    # -- convenience views --------------------------------------------------

    def counted(self) -> List[Decl]:
        """The declarations the queries count: everything outside `com.rosetta`."""
        return [d for d in self.decls if not d.builtin]

    def of_kind(self, kind: str, include_builtins: bool = False) -> List[Decl]:
        return [d for d in self.decls
                if d.kind == kind and (include_builtins or not d.builtin)]

    def namespaces(self) -> List[str]:
        return sorted({d.namespace for d in self.decls if d.namespace})


# --------------------------------------------------------------------------
# Q1 - counts by kind
# --------------------------------------------------------------------------

def q1_counts_by_kind(index: ModelIndex) -> Dict[str, int]:
    """Counts by kind over non-builtin declarations.

    `rules` counts REPORTING rules only, per CONTRACTS section 5 and the JVM
    lanes' shared Queries.java (`RosettaRule.isEligibility()` splits them there;
    the leading REPORTING/ELIGIBILITY token splits them here). The full split is
    still reported by `aux_counts()` so a cross-lane mismatch stays diagnosable.
    """
    counted = index.counted()
    out = {"types": 0, "enums": 0, "functions": 0, "rules": 0}
    for d in counted:
        if d.kind == KIND_TYPE:
            out["types"] += 1
        elif d.kind == KIND_ENUM:
            out["enums"] += 1
        elif d.kind == KIND_FUNC:
            out["functions"] += 1
        elif d.kind == KIND_RULE and not d.eligibility:
            out["rules"] += 1
    return out


# --------------------------------------------------------------------------
# Q2 - data types carrying any [metadata ...] annotation
# --------------------------------------------------------------------------

def q2_metadata_annotated_types(index: ModelIndex) -> Dict[str, int]:
    n = sum(1 for d in index.of_kind(KIND_TYPE) if d.has_metadata())
    return {"metaAnnotatedTypes": n}


def q2_metadata_annotated_type_names(index: ModelIndex) -> List[str]:
    """Diagnostic helper for cross-lane reconciliation."""
    return sorted(d.qname for d in index.of_kind(KIND_TYPE) if d.has_metadata())


# --------------------------------------------------------------------------
# Q3 - reverse reference to a target type
# --------------------------------------------------------------------------

def q3_references_to_target(index: ModelIndex, target: str = "Party") -> Dict[str, int]:
    """Distinct data types with >=1 attribute whose declared type simple name == target.

    Scoped to `dataType` declarations only. Function inputs/outputs and
    annotation bodies also parse as `attribute`, but they are not data types.
    """
    hits = 0
    for d in index.of_kind(KIND_TYPE):
        if any(a.type_simple == target for a in d.attributes):
            hits += 1
    return {"referencesToTarget": hits}


def q3_reference_holder_names(index: ModelIndex, target: str = "Party") -> List[str]:
    return sorted(d.qname for d in index.of_kind(KIND_TYPE)
                  if any(a.type_simple == target for a in d.attributes))


# --------------------------------------------------------------------------
# Q4 - deepest extends chain (edges; root = 0)
# --------------------------------------------------------------------------

def _build_type_maps(index: ModelIndex) -> Tuple[Dict[str, Decl], Dict[str, List[Decl]]]:
    """Resolution tables for `extends`, over ALL data types incl. builtins.

    Builtins carry no `type ... extends`, so including them cannot change the
    answer; it only prevents a chain from breaking if that ever changes.
    """
    by_qname: Dict[str, Decl] = {}
    by_simple: Dict[str, List[Decl]] = {}
    for d in index.decls:
        if d.kind != KIND_TYPE:
            continue
        by_qname.setdefault(d.qname, d)
        by_simple.setdefault(d.name, []).append(d)
    return by_qname, by_simple


def _resolve_parent(decl: Decl,
                    by_qname: Dict[str, Decl],
                    by_simple: Dict[str, List[Decl]],
                    stats: Dict[str, int]) -> Optional[Decl]:
    """Resolve `extends` to a Decl.

    Order: exact qualified name -> unique simple name -> same-namespace
    preference -> same-file preference -> first (counted as ambiguous).
    """
    if not decl.extends_simple:
        return None
    if decl.extends_qname:
        hit = by_qname.get(decl.extends_qname)
        if hit is not None:
            return hit
    cands = by_simple.get(decl.extends_simple)
    if not cands:
        stats["unresolvedExtends"] = stats.get("unresolvedExtends", 0) + 1
        return None
    if len(cands) == 1:
        return cands[0]
    for c in cands:
        if c.namespace == decl.namespace:
            return c
    for c in cands:
        if c.file == decl.file:
            return c
    stats["ambiguousExtends"] = stats.get("ambiguousExtends", 0) + 1
    return cands[0]


def q4_deepest_extends_chain(index: ModelIndex) -> Dict[str, int]:
    depth, _path, _stats = _deepest_extends_chain_detail(index)
    return {"deepestExtendsChain": depth}


def _deepest_extends_chain_detail(index: ModelIndex
                                  ) -> Tuple[int, List[str], Dict[str, int]]:
    """Returns (max edge count, the deepest chain child->root, diagnostics)."""
    by_qname, by_simple = _build_type_maps(index)
    stats: Dict[str, int] = {"unresolvedExtends": 0, "ambiguousExtends": 0, "cycles": 0}
    memo: Dict[int, int] = {}      # id(decl) -> depth in edges

    def depth_of(d: Decl, seen: set) -> int:
        key = id(d)
        cached = memo.get(key)
        if cached is not None:
            return cached
        if key in seen:
            stats["cycles"] += 1
            return 0
        seen.add(key)
        parent = _resolve_parent(d, by_qname, by_simple, stats)
        result = 0 if parent is None else 1 + depth_of(parent, seen)
        seen.discard(key)
        memo[key] = result
        return result

    best = 0
    best_decl: Optional[Decl] = None
    for d in index.of_kind(KIND_TYPE):          # non-builtin roots only
        n = depth_of(d, set())
        if n > best:
            best, best_decl = n, d

    path: List[str] = []
    if best_decl is not None:
        cur: Optional[Decl] = best_decl
        guard = 0
        while cur is not None and guard <= best + 1:
            path.append(cur.qname)
            cur = _resolve_parent(cur, by_qname, by_simple, {})
            guard += 1
    return best, path, stats


# --------------------------------------------------------------------------
# Walk statistics + auxiliary counts (dashboard + cross-lane diagnosis)
# --------------------------------------------------------------------------

def walk_stats(index: ModelIndex) -> Dict[str, int]:
    """Statistics about the tree walk that produced the index."""
    return {
        "nodesVisited": index.nodes_visited,
        "declarations": len(index.decls),
        "declarationsCounted": len(index.counted()),
        "attributes": sum(len(d.attributes) for d in index.decls),
        "symbols": len(index.symbols),
        "nameCollisions": index.name_collisions,
        "namespaces": len(index.namespaces()),
        "unknownElements": index.unknown_elements,
    }


def aux_counts(index: ModelIndex, target: str = "Party") -> Dict[str, object]:
    """Everything outside the CONTRACTS `results` block that aids diagnosis."""
    counted = index.counted()
    per_kind: Dict[str, int] = {}
    for d in counted:
        per_kind[d.kind] = per_kind.get(d.kind, 0) + 1
    depth, path, chain_stats = _deepest_extends_chain_detail(index)
    aux: Dict[str, object] = {
        "targetType": target,
        "declsByKind": dict(sorted(per_kind.items())),
        "builtinDeclarations": len(index.decls) - len(counted),
        "deepestExtendsChainPath": path,
        "attributesOnDataTypes": sum(len(d.attributes)
                                     for d in index.of_kind(KIND_TYPE)),
    }
    aux.update(chain_stats)
    aux.update(walk_stats(index))
    return aux


def results_block(index: ModelIndex, target: str = "Party") -> Dict[str, Dict[str, int]]:
    """The CONTRACTS section 4 `results` object - the cross-lane contract."""
    return {
        "q1": q1_counts_by_kind(index),
        "q2": q2_metadata_annotated_types(index),
        "q3": q3_references_to_target(index, target),
        "q4": q4_deepest_extends_chain(index),
    }


# --------------------------------------------------------------------------
# Self-test - a positive control for the query semantics, no ANTLR needed
# --------------------------------------------------------------------------

def _fixture() -> ModelIndex:
    """A hand-built index exercising every Q1-Q4 branch.

    Shape:
      com.rosetta.model      : basicType string            (BUILTIN - never counted)
      cdm.base.staticdata.party : type Party               (no metadata)
      cdm.a : type Root  [metadata key]                    (metadata on the TYPE)
      cdm.a : type Mid   extends Root, attr party: Party   (Party ref; depth 1)
      cdm.a : type Leaf  extends Mid,  attr p2 [metadata scheme]  (metadata on ATTR; depth 2)
      cdm.a : type Lonely (nothing)                        (depth 0)
      cdm.a : enum E, func F, reporting rule R, eligibility rule R2, choice C
    """
    ix = ModelIndex()
    ix.add(Decl(name="string", kind=KIND_BASIC_TYPE, namespace="com.rosetta.model",
                file="builtins/basictypes.rosetta"))
    # A builtin data type that would break Q1/Q2/Q3 if builtins leaked in.
    ix.add(Decl(name="Leaked", kind=KIND_TYPE, namespace="com.rosetta.model",
                file="builtins/basictypes.rosetta",
                annotations=("metadata",),
                attributes=[Attr(name="p", type_qname="Party", type_simple="Party")]))
    ix.add(Decl(name="Party", kind=KIND_TYPE,
                namespace="cdm.base.staticdata.party", file="cdm/party.rosetta"))
    ix.add(Decl(name="Root", kind=KIND_TYPE, namespace="cdm.a", file="cdm/a.rosetta",
                annotations=("metadata",), annotation_quals=("key",)))
    ix.add(Decl(name="Mid", kind=KIND_TYPE, namespace="cdm.a", file="cdm/a.rosetta",
                extends_qname="Root", extends_simple="Root",
                attributes=[Attr(name="party",
                                 type_qname="cdm.base.staticdata.party.Party",
                                 type_simple="Party")]))
    ix.add(Decl(name="Leaf", kind=KIND_TYPE, namespace="cdm.a", file="cdm/a.rosetta",
                extends_qname="Mid", extends_simple="Mid",
                attributes=[Attr(name="p2", type_qname="Party", type_simple="Party",
                                 annotations=("metadata",),
                                 annotation_quals=("scheme",))]))
    ix.add(Decl(name="Lonely", kind=KIND_TYPE, namespace="cdm.a", file="cdm/a.rosetta"))
    ix.add(Decl(name="E", kind=KIND_ENUM, namespace="cdm.a", file="cdm/a.rosetta"))
    ix.add(Decl(name="F", kind=KIND_FUNC, namespace="cdm.a", file="cdm/a.rosetta",
                attributes=[Attr(name="in1", type_qname="Party", type_simple="Party")]))
    ix.add(Decl(name="R", kind=KIND_RULE, namespace="drr.x", file="drr/r.rosetta"))
    ix.add(Decl(name="R2", kind=KIND_RULE, namespace="drr.x", file="drr/r.rosetta",
                eligibility=True))
    ix.add(Decl(name="C", kind=KIND_CHOICE, namespace="cdm.a", file="cdm/a.rosetta"))
    ix.nodes_visited = 1234
    ix.files = 4
    return ix


def _selftest() -> int:
    ix = _fixture()
    failures: List[str] = []

    def check(label: str, actual, expected) -> None:
        if actual != expected:
            failures.append(f"{label}: expected {expected!r}, got {actual!r}")

    # Q1 - builtins excluded; choice/basicType are NOT data types; the
    # eligibility rule R2 is excluded from `rules` (reporting only).
    check("q1", q1_counts_by_kind(ix),
          {"types": 5, "enums": 1, "functions": 1, "rules": 1})
    # Q2 - Root (type-level) + Leaf (attribute-level). Builtin "Leaked" excluded.
    check("q2", q2_metadata_annotated_types(ix), {"metaAnnotatedTypes": 2})
    check("q2names", q2_metadata_annotated_type_names(ix), ["cdm.a.Leaf", "cdm.a.Root"])
    # Q3 - Mid + Leaf. Func F's input is NOT a data type attribute; builtin excluded.
    check("q3", q3_references_to_target(ix, "Party"), {"referencesToTarget": 2})
    check("q3-none", q3_references_to_target(ix, "NoSuchType"), {"referencesToTarget": 0})
    # Q4 - Leaf -> Mid -> Root = 2 edges.
    check("q4", q4_deepest_extends_chain(ix), {"deepestExtendsChain": 2})
    depth, path, stats = _deepest_extends_chain_detail(ix)
    check("q4path", path, ["cdm.a.Leaf", "cdm.a.Mid", "cdm.a.Root"])
    check("q4cycles", stats["cycles"], 0)
    check("walk", walk_stats(ix)["nodesVisited"], 1234)

    # Negative controls: the checks above must be capable of failing.
    ix2 = _fixture()
    ix2.decls[3].annotations = ()            # drop [metadata key] from Root
    check("neg-q2", q2_metadata_annotated_types(ix2), {"metaAnnotatedTypes": 1})
    ix3 = _fixture()
    ix3.decls[5].extends_simple = None       # break Leaf -> Mid; deepest becomes Mid -> Root
    ix3.decls[5].extends_qname = None
    check("neg-q4", q4_deepest_extends_chain(ix3), {"deepestExtendsChain": 1})
    ix4 = _fixture()                          # cycle guard: Root extends Leaf
    ix4.decls[3].extends_simple = "Leaf"
    ix4.decls[3].extends_qname = "Leaf"
    d4, _p4, s4 = _deepest_extends_chain_detail(ix4)
    if s4["cycles"] < 1:
        failures.append("neg-cycle: expected the cycle guard to fire")

    for f in failures:
        print("FAIL " + f)
    total = 12
    print(f"queries.py selftest: {total - len(failures)}/{total} checks passed")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(_selftest())
