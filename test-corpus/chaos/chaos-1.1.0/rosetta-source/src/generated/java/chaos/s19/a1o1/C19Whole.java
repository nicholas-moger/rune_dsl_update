package chaos.s19.a1o1;

import chaos.s19.a1o1.meta.C19WholeMeta;
import chaos.s19.a1o1.metafields.ReferenceWithMetaC19Part;
import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.annotations.RuneScopedAttributeKey;
import com.rosetta.model.lib.annotations.RuneScopedAttributeReference;
import com.rosetta.model.lib.meta.Key;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.model.metafields.ReferenceWithMetaString;
import com.rosetta.util.ListEquals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Constructor target: every field-shape kind (charter 2.2b), incl. location/address meta at ctor-adjacent seats.
 * @version 1.0.0
 */
@RosettaDataType(value="C19Whole", builder=C19Whole.C19WholeBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C19Whole", model="chaos", builder=C19Whole.C19WholeBuilderImpl.class, version="1.0.0")
public interface C19Whole extends RosettaModelObject {

	C19WholeMeta metaData = new C19WholeMeta();

	/*********************** Getter Methods  ***********************/
	String getName();
	BigDecimal getOpt();
	List<BigDecimal> getScores();
	C19Part getPart();
	ReferenceWithMetaC19Part getPartRef();
	List<? extends C19Part> getParts();
	FieldWithMetaString getSpot();
	ReferenceWithMetaString getPtr();

	/*********************** Build Methods  ***********************/
	C19Whole build();
	
	C19Whole.C19WholeBuilder toBuilder();
	
	static C19Whole.C19WholeBuilder builder() {
		return new C19Whole.C19WholeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C19Whole> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C19Whole> getType() {
		return C19Whole.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
		processor.processBasic(path.newSubPath("opt"), BigDecimal.class, getOpt(), this);
		processor.processBasic(path.newSubPath("scores"), BigDecimal.class, getScores(), this);
		processRosetta(path.newSubPath("part"), processor, C19Part.class, getPart());
		processRosetta(path.newSubPath("partRef"), processor, ReferenceWithMetaC19Part.class, getPartRef());
		processRosetta(path.newSubPath("parts"), processor, C19Part.class, getParts());
		processRosetta(path.newSubPath("spot"), processor, FieldWithMetaString.class, getSpot());
		processRosetta(path.newSubPath("ptr"), processor, ReferenceWithMetaString.class, getPtr());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C19WholeBuilder extends C19Whole, RosettaModelObjectBuilder {
		C19Part.C19PartBuilder getOrCreatePart();
		@Override
		C19Part.C19PartBuilder getPart();
		ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder getOrCreatePartRef();
		@Override
		ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder getPartRef();
		C19Part.C19PartBuilder getOrCreateParts(int index);
		@Override
		List<? extends C19Part.C19PartBuilder> getParts();
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateSpot();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getSpot();
		ReferenceWithMetaString.ReferenceWithMetaStringBuilder getOrCreatePtr();
		@Override
		ReferenceWithMetaString.ReferenceWithMetaStringBuilder getPtr();
		C19Whole.C19WholeBuilder setName(String name);
		C19Whole.C19WholeBuilder setOpt(BigDecimal opt);
		C19Whole.C19WholeBuilder addScores(BigDecimal scores);
		C19Whole.C19WholeBuilder addScores(BigDecimal scores, int idx);
		C19Whole.C19WholeBuilder addScores(List<BigDecimal> scores);
		C19Whole.C19WholeBuilder setScores(List<BigDecimal> scores);
		C19Whole.C19WholeBuilder setPart(C19Part part);
		C19Whole.C19WholeBuilder setPartRef(ReferenceWithMetaC19Part partRef);
		C19Whole.C19WholeBuilder setPartRefValue(C19Part partRef);
		C19Whole.C19WholeBuilder addParts(C19Part parts);
		C19Whole.C19WholeBuilder addParts(C19Part parts, int idx);
		C19Whole.C19WholeBuilder addParts(List<? extends C19Part> parts);
		C19Whole.C19WholeBuilder setParts(List<? extends C19Part> parts);
		C19Whole.C19WholeBuilder setSpot(FieldWithMetaString spot);
		C19Whole.C19WholeBuilder setSpotValue(String spot);
		C19Whole.C19WholeBuilder setPtr(ReferenceWithMetaString ptr);
		C19Whole.C19WholeBuilder setPtrValue(String ptr);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
			processor.processBasic(path.newSubPath("opt"), BigDecimal.class, getOpt(), this);
			processor.processBasic(path.newSubPath("scores"), BigDecimal.class, getScores(), this);
			processRosetta(path.newSubPath("part"), processor, C19Part.C19PartBuilder.class, getPart());
			processRosetta(path.newSubPath("partRef"), processor, ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder.class, getPartRef());
			processRosetta(path.newSubPath("parts"), processor, C19Part.C19PartBuilder.class, getParts());
			processRosetta(path.newSubPath("spot"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getSpot());
			processRosetta(path.newSubPath("ptr"), processor, ReferenceWithMetaString.ReferenceWithMetaStringBuilder.class, getPtr());
		}
		

		C19Whole.C19WholeBuilder prune();
	}

	/*********************** Immutable Implementation of C19Whole  ***********************/
	class C19WholeImpl implements C19Whole {
		private final String name;
		private final BigDecimal opt;
		private final List<BigDecimal> scores;
		private final C19Part part;
		private final ReferenceWithMetaC19Part partRef;
		private final List<? extends C19Part> parts;
		private final FieldWithMetaString spot;
		private final ReferenceWithMetaString ptr;
		
		protected C19WholeImpl(C19Whole.C19WholeBuilder builder) {
			this.name = builder.getName();
			this.opt = builder.getOpt();
			this.scores = ofNullable(builder.getScores()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.part = ofNullable(builder.getPart()).map(f->f.build()).orElse(null);
			this.partRef = ofNullable(builder.getPartRef()).map(f->f.build()).orElse(null);
			this.parts = ofNullable(builder.getParts()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
			this.spot = ofNullable(builder.getSpot()).map(f->f.build()).orElse(null);
			this.ptr = ofNullable(builder.getPtr()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("name")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("name")
		public String getName() {
			return name;
		}
		
		@Override
		@RosettaAttribute("opt")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("opt")
		public BigDecimal getOpt() {
			return opt;
		}
		
		@Override
		@RosettaAttribute("scores")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("scores")
		public List<BigDecimal> getScores() {
			return scores;
		}
		
		@Override
		@RosettaAttribute("part")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("part")
		public C19Part getPart() {
			return part;
		}
		
		@Override
		@RosettaAttribute("partRef")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("partRef")
		public ReferenceWithMetaC19Part getPartRef() {
			return partRef;
		}
		
		@Override
		@RosettaAttribute("parts")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("parts")
		public List<? extends C19Part> getParts() {
			return parts;
		}
		
		@Override
		@RosettaAttribute("spot")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("spot")
		@RuneScopedAttributeKey
		public FieldWithMetaString getSpot() {
			return spot;
		}
		
		@Override
		@RosettaAttribute("ptr")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("ptr")
		@RuneScopedAttributeReference
		public ReferenceWithMetaString getPtr() {
			return ptr;
		}
		
		@Override
		public C19Whole build() {
			return this;
		}
		
		@Override
		public C19Whole.C19WholeBuilder toBuilder() {
			C19Whole.C19WholeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C19Whole.C19WholeBuilder builder) {
			ofNullable(getName()).ifPresent(builder::setName);
			ofNullable(getOpt()).ifPresent(builder::setOpt);
			ofNullable(getScores()).ifPresent(builder::setScores);
			ofNullable(getPart()).ifPresent(builder::setPart);
			ofNullable(getPartRef()).ifPresent(builder::setPartRef);
			ofNullable(getParts()).ifPresent(builder::setParts);
			ofNullable(getSpot()).ifPresent(builder::setSpot);
			ofNullable(getPtr()).ifPresent(builder::setPtr);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C19Whole _that = getType().cast(o);
		
			if (!Objects.equals(name, _that.getName())) return false;
			if (!Objects.equals(opt, _that.getOpt())) return false;
			if (!ListEquals.listEquals(scores, _that.getScores())) return false;
			if (!Objects.equals(part, _that.getPart())) return false;
			if (!Objects.equals(partRef, _that.getPartRef())) return false;
			if (!ListEquals.listEquals(parts, _that.getParts())) return false;
			if (!Objects.equals(spot, _that.getSpot())) return false;
			if (!Objects.equals(ptr, _that.getPtr())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			_result = 31 * _result + (opt != null ? opt.hashCode() : 0);
			_result = 31 * _result + (scores != null ? scores.hashCode() : 0);
			_result = 31 * _result + (part != null ? part.hashCode() : 0);
			_result = 31 * _result + (partRef != null ? partRef.hashCode() : 0);
			_result = 31 * _result + (parts != null ? parts.hashCode() : 0);
			_result = 31 * _result + (spot != null ? spot.hashCode() : 0);
			_result = 31 * _result + (ptr != null ? ptr.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C19Whole {" +
				"name=" + this.name + ", " +
				"opt=" + this.opt + ", " +
				"scores=" + this.scores + ", " +
				"part=" + this.part + ", " +
				"partRef=" + this.partRef + ", " +
				"parts=" + this.parts + ", " +
				"spot=" + this.spot + ", " +
				"ptr=" + this.ptr +
			'}';
		}
	}

	/*********************** Builder Implementation of C19Whole  ***********************/
	class C19WholeBuilderImpl implements C19Whole.C19WholeBuilder {
	
		protected String name;
		protected BigDecimal opt;
		protected List<BigDecimal> scores = new ArrayList<>();
		protected C19Part.C19PartBuilder part;
		protected ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder partRef;
		protected List<C19Part.C19PartBuilder> parts = new ArrayList<>();
		protected FieldWithMetaString.FieldWithMetaStringBuilder spot;
		protected ReferenceWithMetaString.ReferenceWithMetaStringBuilder ptr;
		
		@Override
		@RosettaAttribute("name")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("name")
		public String getName() {
			return name;
		}
		
		@Override
		@RosettaAttribute("opt")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("opt")
		public BigDecimal getOpt() {
			return opt;
		}
		
		@Override
		@RosettaAttribute("scores")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("scores")
		public List<BigDecimal> getScores() {
			return scores;
		}
		
		@Override
		@RosettaAttribute("part")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("part")
		public C19Part.C19PartBuilder getPart() {
			return part;
		}
		
		@Override
		public C19Part.C19PartBuilder getOrCreatePart() {
			C19Part.C19PartBuilder result;
			if (part!=null) {
				result = part;
			}
			else {
				result = part = C19Part.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("partRef")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("partRef")
		public ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder getPartRef() {
			return partRef;
		}
		
		@Override
		public ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder getOrCreatePartRef() {
			ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder result;
			if (partRef!=null) {
				result = partRef;
			}
			else {
				result = partRef = ReferenceWithMetaC19Part.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("parts")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("parts")
		public List<? extends C19Part.C19PartBuilder> getParts() {
			return parts;
		}
		
		@Override
		public C19Part.C19PartBuilder getOrCreateParts(int index) {
			if (parts==null) {
				this.parts = new ArrayList<>();
			}
			return getIndex(parts, index, () -> {
						C19Part.C19PartBuilder newParts = C19Part.builder();
						return newParts;
					});
		}
		
		@Override
		@RosettaAttribute("spot")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("spot")
		@RuneScopedAttributeKey
		public FieldWithMetaString.FieldWithMetaStringBuilder getSpot() {
			return spot;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateSpot() {
			FieldWithMetaString.FieldWithMetaStringBuilder result;
			if (spot!=null) {
				result = spot;
			}
			else {
				result = spot = FieldWithMetaString.builder();
				result.getOrCreateMeta().toBuilder().addKey(Key.builder().setScope("DOCUMENT"));
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("ptr")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("ptr")
		@RuneScopedAttributeReference
		public ReferenceWithMetaString.ReferenceWithMetaStringBuilder getPtr() {
			return ptr;
		}
		
		@Override
		public ReferenceWithMetaString.ReferenceWithMetaStringBuilder getOrCreatePtr() {
			ReferenceWithMetaString.ReferenceWithMetaStringBuilder result;
			if (ptr!=null) {
				result = ptr;
			}
			else {
				result = ptr = ReferenceWithMetaString.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("name")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("name")
		@Override
		public C19Whole.C19WholeBuilder setName(String _name) {
			this.name = _name == null ? null : _name;
			return this;
		}
		
		@RosettaAttribute("opt")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("opt")
		@Override
		public C19Whole.C19WholeBuilder setOpt(BigDecimal _opt) {
			this.opt = _opt == null ? null : _opt;
			return this;
		}
		
		@RosettaAttribute("scores")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("scores")
		@Override
		public C19Whole.C19WholeBuilder addScores(BigDecimal _scores) {
			if (_scores != null) {
				this.scores.add(_scores);
			}
			return this;
		}
		
		@Override
		public C19Whole.C19WholeBuilder addScores(BigDecimal _scores, int idx) {
			getIndex(this.scores, idx, () -> _scores);
			return this;
		}
		
		@Override
		public C19Whole.C19WholeBuilder addScores(List<BigDecimal> scoress) {
			if (scoress != null) {
				for (final BigDecimal toAdd : scoress) {
					this.scores.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("scores")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("scores")
		@Override
		public C19Whole.C19WholeBuilder setScores(List<BigDecimal> scoress) {
			if (scoress == null) {
				this.scores = new ArrayList<>();
			} else {
				this.scores = scoress.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("part")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("part")
		@Override
		public C19Whole.C19WholeBuilder setPart(C19Part _part) {
			this.part = _part == null ? null : _part.toBuilder();
			return this;
		}
		
		@RosettaAttribute("partRef")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("partRef")
		@Override
		public C19Whole.C19WholeBuilder setPartRef(ReferenceWithMetaC19Part _partRef) {
			this.partRef = _partRef == null ? null : _partRef.toBuilder();
			return this;
		}
		
		@Override
		public C19Whole.C19WholeBuilder setPartRefValue(C19Part _partRef) {
			this.getOrCreatePartRef().setValue(_partRef);
			return this;
		}
		
		@RosettaAttribute("parts")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("parts")
		@Override
		public C19Whole.C19WholeBuilder addParts(C19Part _parts) {
			if (_parts != null) {
				this.parts.add(_parts.toBuilder());
			}
			return this;
		}
		
		@Override
		public C19Whole.C19WholeBuilder addParts(C19Part _parts, int idx) {
			getIndex(this.parts, idx, () -> _parts.toBuilder());
			return this;
		}
		
		@Override
		public C19Whole.C19WholeBuilder addParts(List<? extends C19Part> partss) {
			if (partss != null) {
				for (final C19Part toAdd : partss) {
					this.parts.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("parts")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("parts")
		@Override
		public C19Whole.C19WholeBuilder setParts(List<? extends C19Part> partss) {
			if (partss == null) {
				this.parts = new ArrayList<>();
			} else {
				this.parts = partss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("spot")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("spot")
		@RuneScopedAttributeKey
		@Override
		public C19Whole.C19WholeBuilder setSpot(FieldWithMetaString _spot) {
			this.spot = _spot == null ? null : _spot.toBuilder();
			return this;
		}
		
		@Override
		public C19Whole.C19WholeBuilder setSpotValue(String _spot) {
			this.getOrCreateSpot().setValue(_spot);
			return this;
		}
		
		@RosettaAttribute("ptr")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("ptr")
		@RuneScopedAttributeReference
		@Override
		public C19Whole.C19WholeBuilder setPtr(ReferenceWithMetaString _ptr) {
			this.ptr = _ptr == null ? null : _ptr.toBuilder();
			return this;
		}
		
		@Override
		public C19Whole.C19WholeBuilder setPtrValue(String _ptr) {
			this.getOrCreatePtr().setValue(_ptr);
			return this;
		}
		
		@Override
		public C19Whole build() {
			return new C19Whole.C19WholeImpl(this);
		}
		
		@Override
		public C19Whole.C19WholeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C19Whole.C19WholeBuilder prune() {
			if (part!=null && !part.prune().hasData()) part = null;
			if (partRef!=null && !partRef.prune().hasData()) partRef = null;
			parts = parts.stream().filter(b->b!=null).<C19Part.C19PartBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			if (spot!=null && !spot.prune().hasData()) spot = null;
			if (ptr!=null && !ptr.prune().hasData()) ptr = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getName()!=null) return true;
			if (getOpt()!=null) return true;
			if (getScores()!=null && !getScores().isEmpty()) return true;
			if (getPart()!=null && getPart().hasData()) return true;
			if (getPartRef()!=null && getPartRef().hasData()) return true;
			if (getParts()!=null && getParts().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			if (getSpot()!=null) return true;
			if (getPtr()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C19Whole.C19WholeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C19Whole.C19WholeBuilder o = (C19Whole.C19WholeBuilder) other;
			
			merger.mergeRosetta(getPart(), o.getPart(), this::setPart);
			merger.mergeRosetta(getPartRef(), o.getPartRef(), this::setPartRef);
			merger.mergeRosetta(getParts(), o.getParts(), this::getOrCreateParts);
			merger.mergeRosetta(getSpot(), o.getSpot(), this::setSpot);
			merger.mergeRosetta(getPtr(), o.getPtr(), this::setPtr);
			
			merger.mergeBasic(getName(), o.getName(), this::setName);
			merger.mergeBasic(getOpt(), o.getOpt(), this::setOpt);
			merger.mergeBasic(getScores(), o.getScores(), (Consumer<BigDecimal>) this::addScores);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C19Whole _that = getType().cast(o);
		
			if (!Objects.equals(name, _that.getName())) return false;
			if (!Objects.equals(opt, _that.getOpt())) return false;
			if (!ListEquals.listEquals(scores, _that.getScores())) return false;
			if (!Objects.equals(part, _that.getPart())) return false;
			if (!Objects.equals(partRef, _that.getPartRef())) return false;
			if (!ListEquals.listEquals(parts, _that.getParts())) return false;
			if (!Objects.equals(spot, _that.getSpot())) return false;
			if (!Objects.equals(ptr, _that.getPtr())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			_result = 31 * _result + (opt != null ? opt.hashCode() : 0);
			_result = 31 * _result + (scores != null ? scores.hashCode() : 0);
			_result = 31 * _result + (part != null ? part.hashCode() : 0);
			_result = 31 * _result + (partRef != null ? partRef.hashCode() : 0);
			_result = 31 * _result + (parts != null ? parts.hashCode() : 0);
			_result = 31 * _result + (spot != null ? spot.hashCode() : 0);
			_result = 31 * _result + (ptr != null ? ptr.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C19WholeBuilder {" +
				"name=" + this.name + ", " +
				"opt=" + this.opt + ", " +
				"scores=" + this.scores + ", " +
				"part=" + this.part + ", " +
				"partRef=" + this.partRef + ", " +
				"parts=" + this.parts + ", " +
				"spot=" + this.spot + ", " +
				"ptr=" + this.ptr +
			'}';
		}
	}
}
