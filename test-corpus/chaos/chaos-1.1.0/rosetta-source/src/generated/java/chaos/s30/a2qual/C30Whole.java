package chaos.s30.a2qual;

import chaos.s30.a2qual.h.C30Part;
import chaos.s30.a2qual.h.metafields.ReferenceWithMetaC30Part;
import chaos.s30.a2qual.meta.C30WholeMeta;
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
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.util.ListEquals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * The ctor / set target: plain, meta, reference and nested seats.
 * @version 1.0.0
 */
@RosettaDataType(value="C30Whole", builder=C30Whole.C30WholeBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C30Whole", model="chaos", builder=C30Whole.C30WholeBuilderImpl.class, version="1.0.0")
public interface C30Whole extends RosettaModelObject {

	C30WholeMeta metaData = new C30WholeMeta();

	/*********************** Getter Methods  ***********************/
	String getName();
	FieldWithMetaString getCode();
	List<BigDecimal> getScores();
	C30Part getPart();
	ReferenceWithMetaC30Part getPartRef();
	List<? extends C30Part> getParts();
	C30Whole getNested();

	/*********************** Build Methods  ***********************/
	C30Whole build();
	
	C30Whole.C30WholeBuilder toBuilder();
	
	static C30Whole.C30WholeBuilder builder() {
		return new C30Whole.C30WholeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C30Whole> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C30Whole> getType() {
		return C30Whole.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
		processRosetta(path.newSubPath("code"), processor, FieldWithMetaString.class, getCode());
		processor.processBasic(path.newSubPath("scores"), BigDecimal.class, getScores(), this);
		processRosetta(path.newSubPath("part"), processor, C30Part.class, getPart());
		processRosetta(path.newSubPath("partRef"), processor, ReferenceWithMetaC30Part.class, getPartRef());
		processRosetta(path.newSubPath("parts"), processor, C30Part.class, getParts());
		processRosetta(path.newSubPath("nested"), processor, C30Whole.class, getNested());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C30WholeBuilder extends C30Whole, RosettaModelObjectBuilder {
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCode();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getCode();
		C30Part.C30PartBuilder getOrCreatePart();
		@Override
		C30Part.C30PartBuilder getPart();
		ReferenceWithMetaC30Part.ReferenceWithMetaC30PartBuilder getOrCreatePartRef();
		@Override
		ReferenceWithMetaC30Part.ReferenceWithMetaC30PartBuilder getPartRef();
		C30Part.C30PartBuilder getOrCreateParts(int index);
		@Override
		List<? extends C30Part.C30PartBuilder> getParts();
		C30Whole.C30WholeBuilder getOrCreateNested();
		@Override
		C30Whole.C30WholeBuilder getNested();
		C30Whole.C30WholeBuilder setName(String name);
		C30Whole.C30WholeBuilder setCode(FieldWithMetaString code);
		C30Whole.C30WholeBuilder setCodeValue(String code);
		C30Whole.C30WholeBuilder addScores(BigDecimal scores);
		C30Whole.C30WholeBuilder addScores(BigDecimal scores, int idx);
		C30Whole.C30WholeBuilder addScores(List<BigDecimal> scores);
		C30Whole.C30WholeBuilder setScores(List<BigDecimal> scores);
		C30Whole.C30WholeBuilder setPart(C30Part part);
		C30Whole.C30WholeBuilder setPartRef(ReferenceWithMetaC30Part partRef);
		C30Whole.C30WholeBuilder setPartRefValue(C30Part partRef);
		C30Whole.C30WholeBuilder addParts(C30Part parts);
		C30Whole.C30WholeBuilder addParts(C30Part parts, int idx);
		C30Whole.C30WholeBuilder addParts(List<? extends C30Part> parts);
		C30Whole.C30WholeBuilder setParts(List<? extends C30Part> parts);
		C30Whole.C30WholeBuilder setNested(C30Whole nested);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
			processRosetta(path.newSubPath("code"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getCode());
			processor.processBasic(path.newSubPath("scores"), BigDecimal.class, getScores(), this);
			processRosetta(path.newSubPath("part"), processor, C30Part.C30PartBuilder.class, getPart());
			processRosetta(path.newSubPath("partRef"), processor, ReferenceWithMetaC30Part.ReferenceWithMetaC30PartBuilder.class, getPartRef());
			processRosetta(path.newSubPath("parts"), processor, C30Part.C30PartBuilder.class, getParts());
			processRosetta(path.newSubPath("nested"), processor, C30Whole.C30WholeBuilder.class, getNested());
		}
		

		C30Whole.C30WholeBuilder prune();
	}

	/*********************** Immutable Implementation of C30Whole  ***********************/
	class C30WholeImpl implements C30Whole {
		private final String name;
		private final FieldWithMetaString code;
		private final List<BigDecimal> scores;
		private final C30Part part;
		private final ReferenceWithMetaC30Part partRef;
		private final List<? extends C30Part> parts;
		private final C30Whole nested;
		
		protected C30WholeImpl(C30Whole.C30WholeBuilder builder) {
			this.name = builder.getName();
			this.code = ofNullable(builder.getCode()).map(f->f.build()).orElse(null);
			this.scores = ofNullable(builder.getScores()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.part = ofNullable(builder.getPart()).map(f->f.build()).orElse(null);
			this.partRef = ofNullable(builder.getPartRef()).map(f->f.build()).orElse(null);
			this.parts = ofNullable(builder.getParts()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
			this.nested = ofNullable(builder.getNested()).map(f->f.build()).orElse(null);
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
		@RosettaAttribute("code")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("code")
		public FieldWithMetaString getCode() {
			return code;
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
		public C30Part getPart() {
			return part;
		}
		
		@Override
		@RosettaAttribute("partRef")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("partRef")
		public ReferenceWithMetaC30Part getPartRef() {
			return partRef;
		}
		
		@Override
		@RosettaAttribute("parts")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("parts")
		public List<? extends C30Part> getParts() {
			return parts;
		}
		
		@Override
		@RosettaAttribute("nested")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("nested")
		public C30Whole getNested() {
			return nested;
		}
		
		@Override
		public C30Whole build() {
			return this;
		}
		
		@Override
		public C30Whole.C30WholeBuilder toBuilder() {
			C30Whole.C30WholeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C30Whole.C30WholeBuilder builder) {
			ofNullable(getName()).ifPresent(builder::setName);
			ofNullable(getCode()).ifPresent(builder::setCode);
			ofNullable(getScores()).ifPresent(builder::setScores);
			ofNullable(getPart()).ifPresent(builder::setPart);
			ofNullable(getPartRef()).ifPresent(builder::setPartRef);
			ofNullable(getParts()).ifPresent(builder::setParts);
			ofNullable(getNested()).ifPresent(builder::setNested);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C30Whole _that = getType().cast(o);
		
			if (!Objects.equals(name, _that.getName())) return false;
			if (!Objects.equals(code, _that.getCode())) return false;
			if (!ListEquals.listEquals(scores, _that.getScores())) return false;
			if (!Objects.equals(part, _that.getPart())) return false;
			if (!Objects.equals(partRef, _that.getPartRef())) return false;
			if (!ListEquals.listEquals(parts, _that.getParts())) return false;
			if (!Objects.equals(nested, _that.getNested())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			_result = 31 * _result + (code != null ? code.hashCode() : 0);
			_result = 31 * _result + (scores != null ? scores.hashCode() : 0);
			_result = 31 * _result + (part != null ? part.hashCode() : 0);
			_result = 31 * _result + (partRef != null ? partRef.hashCode() : 0);
			_result = 31 * _result + (parts != null ? parts.hashCode() : 0);
			_result = 31 * _result + (nested != null ? nested.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C30Whole {" +
				"name=" + this.name + ", " +
				"code=" + this.code + ", " +
				"scores=" + this.scores + ", " +
				"part=" + this.part + ", " +
				"partRef=" + this.partRef + ", " +
				"parts=" + this.parts + ", " +
				"nested=" + this.nested +
			'}';
		}
	}

	/*********************** Builder Implementation of C30Whole  ***********************/
	class C30WholeBuilderImpl implements C30Whole.C30WholeBuilder {
	
		protected String name;
		protected FieldWithMetaString.FieldWithMetaStringBuilder code;
		protected List<BigDecimal> scores = new ArrayList<>();
		protected C30Part.C30PartBuilder part;
		protected ReferenceWithMetaC30Part.ReferenceWithMetaC30PartBuilder partRef;
		protected List<C30Part.C30PartBuilder> parts = new ArrayList<>();
		protected C30Whole.C30WholeBuilder nested;
		
		@Override
		@RosettaAttribute("name")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("name")
		public String getName() {
			return name;
		}
		
		@Override
		@RosettaAttribute("code")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("code")
		public FieldWithMetaString.FieldWithMetaStringBuilder getCode() {
			return code;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCode() {
			FieldWithMetaString.FieldWithMetaStringBuilder result;
			if (code!=null) {
				result = code;
			}
			else {
				result = code = FieldWithMetaString.builder();
			}
			
			return result;
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
		public C30Part.C30PartBuilder getPart() {
			return part;
		}
		
		@Override
		public C30Part.C30PartBuilder getOrCreatePart() {
			C30Part.C30PartBuilder result;
			if (part!=null) {
				result = part;
			}
			else {
				result = part = C30Part.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("partRef")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("partRef")
		public ReferenceWithMetaC30Part.ReferenceWithMetaC30PartBuilder getPartRef() {
			return partRef;
		}
		
		@Override
		public ReferenceWithMetaC30Part.ReferenceWithMetaC30PartBuilder getOrCreatePartRef() {
			ReferenceWithMetaC30Part.ReferenceWithMetaC30PartBuilder result;
			if (partRef!=null) {
				result = partRef;
			}
			else {
				result = partRef = ReferenceWithMetaC30Part.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("parts")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("parts")
		public List<? extends C30Part.C30PartBuilder> getParts() {
			return parts;
		}
		
		@Override
		public C30Part.C30PartBuilder getOrCreateParts(int index) {
			if (parts==null) {
				this.parts = new ArrayList<>();
			}
			return getIndex(parts, index, () -> {
						C30Part.C30PartBuilder newParts = C30Part.builder();
						return newParts;
					});
		}
		
		@Override
		@RosettaAttribute("nested")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("nested")
		public C30Whole.C30WholeBuilder getNested() {
			return nested;
		}
		
		@Override
		public C30Whole.C30WholeBuilder getOrCreateNested() {
			C30Whole.C30WholeBuilder result;
			if (nested!=null) {
				result = nested;
			}
			else {
				result = nested = C30Whole.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("name")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("name")
		@Override
		public C30Whole.C30WholeBuilder setName(String _name) {
			this.name = _name == null ? null : _name;
			return this;
		}
		
		@RosettaAttribute("code")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("code")
		@Override
		public C30Whole.C30WholeBuilder setCode(FieldWithMetaString _code) {
			this.code = _code == null ? null : _code.toBuilder();
			return this;
		}
		
		@Override
		public C30Whole.C30WholeBuilder setCodeValue(String _code) {
			this.getOrCreateCode().setValue(_code);
			return this;
		}
		
		@RosettaAttribute("scores")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("scores")
		@Override
		public C30Whole.C30WholeBuilder addScores(BigDecimal _scores) {
			if (_scores != null) {
				this.scores.add(_scores);
			}
			return this;
		}
		
		@Override
		public C30Whole.C30WholeBuilder addScores(BigDecimal _scores, int idx) {
			getIndex(this.scores, idx, () -> _scores);
			return this;
		}
		
		@Override
		public C30Whole.C30WholeBuilder addScores(List<BigDecimal> scoress) {
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
		public C30Whole.C30WholeBuilder setScores(List<BigDecimal> scoress) {
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
		public C30Whole.C30WholeBuilder setPart(C30Part _part) {
			this.part = _part == null ? null : _part.toBuilder();
			return this;
		}
		
		@RosettaAttribute("partRef")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("partRef")
		@Override
		public C30Whole.C30WholeBuilder setPartRef(ReferenceWithMetaC30Part _partRef) {
			this.partRef = _partRef == null ? null : _partRef.toBuilder();
			return this;
		}
		
		@Override
		public C30Whole.C30WholeBuilder setPartRefValue(C30Part _partRef) {
			this.getOrCreatePartRef().setValue(_partRef);
			return this;
		}
		
		@RosettaAttribute("parts")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("parts")
		@Override
		public C30Whole.C30WholeBuilder addParts(C30Part _parts) {
			if (_parts != null) {
				this.parts.add(_parts.toBuilder());
			}
			return this;
		}
		
		@Override
		public C30Whole.C30WholeBuilder addParts(C30Part _parts, int idx) {
			getIndex(this.parts, idx, () -> _parts.toBuilder());
			return this;
		}
		
		@Override
		public C30Whole.C30WholeBuilder addParts(List<? extends C30Part> partss) {
			if (partss != null) {
				for (final C30Part toAdd : partss) {
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
		public C30Whole.C30WholeBuilder setParts(List<? extends C30Part> partss) {
			if (partss == null) {
				this.parts = new ArrayList<>();
			} else {
				this.parts = partss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("nested")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("nested")
		@Override
		public C30Whole.C30WholeBuilder setNested(C30Whole _nested) {
			this.nested = _nested == null ? null : _nested.toBuilder();
			return this;
		}
		
		@Override
		public C30Whole build() {
			return new C30Whole.C30WholeImpl(this);
		}
		
		@Override
		public C30Whole.C30WholeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C30Whole.C30WholeBuilder prune() {
			if (code!=null && !code.prune().hasData()) code = null;
			if (part!=null && !part.prune().hasData()) part = null;
			if (partRef!=null && !partRef.prune().hasData()) partRef = null;
			parts = parts.stream().filter(b->b!=null).<C30Part.C30PartBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			if (nested!=null && !nested.prune().hasData()) nested = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getName()!=null) return true;
			if (getCode()!=null) return true;
			if (getScores()!=null && !getScores().isEmpty()) return true;
			if (getPart()!=null && getPart().hasData()) return true;
			if (getPartRef()!=null && getPartRef().hasData()) return true;
			if (getParts()!=null && getParts().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			if (getNested()!=null && getNested().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C30Whole.C30WholeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C30Whole.C30WholeBuilder o = (C30Whole.C30WholeBuilder) other;
			
			merger.mergeRosetta(getCode(), o.getCode(), this::setCode);
			merger.mergeRosetta(getPart(), o.getPart(), this::setPart);
			merger.mergeRosetta(getPartRef(), o.getPartRef(), this::setPartRef);
			merger.mergeRosetta(getParts(), o.getParts(), this::getOrCreateParts);
			merger.mergeRosetta(getNested(), o.getNested(), this::setNested);
			
			merger.mergeBasic(getName(), o.getName(), this::setName);
			merger.mergeBasic(getScores(), o.getScores(), (Consumer<BigDecimal>) this::addScores);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C30Whole _that = getType().cast(o);
		
			if (!Objects.equals(name, _that.getName())) return false;
			if (!Objects.equals(code, _that.getCode())) return false;
			if (!ListEquals.listEquals(scores, _that.getScores())) return false;
			if (!Objects.equals(part, _that.getPart())) return false;
			if (!Objects.equals(partRef, _that.getPartRef())) return false;
			if (!ListEquals.listEquals(parts, _that.getParts())) return false;
			if (!Objects.equals(nested, _that.getNested())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			_result = 31 * _result + (code != null ? code.hashCode() : 0);
			_result = 31 * _result + (scores != null ? scores.hashCode() : 0);
			_result = 31 * _result + (part != null ? part.hashCode() : 0);
			_result = 31 * _result + (partRef != null ? partRef.hashCode() : 0);
			_result = 31 * _result + (parts != null ? parts.hashCode() : 0);
			_result = 31 * _result + (nested != null ? nested.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C30WholeBuilder {" +
				"name=" + this.name + ", " +
				"code=" + this.code + ", " +
				"scores=" + this.scores + ", " +
				"part=" + this.part + ", " +
				"partRef=" + this.partRef + ", " +
				"parts=" + this.parts + ", " +
				"nested=" + this.nested +
			'}';
		}
	}
}
