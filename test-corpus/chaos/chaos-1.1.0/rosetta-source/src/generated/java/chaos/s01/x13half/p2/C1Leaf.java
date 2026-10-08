package chaos.s01.x13half.p2;

import chaos.s01.x13half.p1.C1Ref;
import chaos.s01.x13half.p2.meta.C1LeafMeta;
import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RosettaIgnore;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.annotations.RuneIgnore;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.lib.records.Date;
import com.rosetta.util.ListEquals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Leaf - overrides, parameterised types, multi-cardinality.
 * @version 1.0.0
 */
@RosettaDataType(value="C1Leaf", builder=C1Leaf.C1LeafBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C1Leaf", model="chaos", builder=C1Leaf.C1LeafBuilderImpl.class, version="1.0.0")
public interface C1Leaf extends C1Mid {

	C1LeafMeta metaData = new C1LeafMeta();

	/*********************** Getter Methods  ***********************/
	@Override
	String getNote();
	List<? extends C1Ref> getParts();
	BigDecimal getRatio();
	String getCode();

	/*********************** Build Methods  ***********************/
	C1Leaf build();
	
	C1Leaf.C1LeafBuilder toBuilder();
	
	static C1Leaf.C1LeafBuilder builder() {
		return new C1Leaf.C1LeafBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C1Leaf> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C1Leaf> getType() {
		return C1Leaf.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("baseId"), String.class, getBaseId(), this);
		processor.processBasic(path.newSubPath("note"), String.class, getNote(), this);
		processor.processBasic(path.newSubPath("mids"), BigDecimal.class, getMids(), this);
		processor.processBasic(path.newSubPath("asOf"), Date.class, getAsOf(), this);
		processRosetta(path.newSubPath("parts"), processor, C1Ref.class, getParts());
		processor.processBasic(path.newSubPath("ratio"), BigDecimal.class, getRatio(), this);
		processor.processBasic(path.newSubPath("code"), String.class, getCode(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C1LeafBuilder extends C1Leaf, C1Mid.C1MidBuilder {
		C1Ref.C1RefBuilder getOrCreateParts(int index);
		@Override
		List<? extends C1Ref.C1RefBuilder> getParts();
		@Override
		C1Leaf.C1LeafBuilder setBaseId(String baseId);
		@Override
		C1Leaf.C1LeafBuilder setNote(String note);
		@Override
		C1Leaf.C1LeafBuilder addMids(BigDecimal mids);
		@Override
		C1Leaf.C1LeafBuilder addMids(BigDecimal mids, int idx);
		@Override
		C1Leaf.C1LeafBuilder addMids(List<BigDecimal> mids);
		@Override
		C1Leaf.C1LeafBuilder setMids(List<BigDecimal> mids);
		@Override
		C1Leaf.C1LeafBuilder setAsOf(Date asOf);
		C1Leaf.C1LeafBuilder setNoteOverriddenAsString(String note);
		C1Leaf.C1LeafBuilder addParts(C1Ref parts);
		C1Leaf.C1LeafBuilder addParts(C1Ref parts, int idx);
		C1Leaf.C1LeafBuilder addParts(List<? extends C1Ref> parts);
		C1Leaf.C1LeafBuilder setParts(List<? extends C1Ref> parts);
		C1Leaf.C1LeafBuilder setRatio(BigDecimal ratio);
		C1Leaf.C1LeafBuilder setCode(String code);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("baseId"), String.class, getBaseId(), this);
			processor.processBasic(path.newSubPath("note"), String.class, getNote(), this);
			processor.processBasic(path.newSubPath("mids"), BigDecimal.class, getMids(), this);
			processor.processBasic(path.newSubPath("asOf"), Date.class, getAsOf(), this);
			processRosetta(path.newSubPath("parts"), processor, C1Ref.C1RefBuilder.class, getParts());
			processor.processBasic(path.newSubPath("ratio"), BigDecimal.class, getRatio(), this);
			processor.processBasic(path.newSubPath("code"), String.class, getCode(), this);
		}
		

		C1Leaf.C1LeafBuilder prune();
	}

	/*********************** Immutable Implementation of C1Leaf  ***********************/
	class C1LeafImpl extends C1Mid.C1MidImpl implements C1Leaf {
		private final String note;
		private final List<? extends C1Ref> parts;
		private final BigDecimal ratio;
		private final String code;
		
		protected C1LeafImpl(C1Leaf.C1LeafBuilder builder) {
			super(builder);
			this.note = builder.getNote();
			this.parts = ofNullable(builder.getParts()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
			this.ratio = builder.getRatio();
			this.code = builder.getCode();
		}
		
		@Override
		@RosettaAttribute("note")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("note")
		public String getNote() {
			return note;
		}
		
		@Override
		@RosettaAttribute("parts")
		@Accessor(AccessorType.GETTER)
		@Required
		@Multi
		@RuneAttribute("parts")
		public List<? extends C1Ref> getParts() {
			return parts;
		}
		
		@Override
		@RosettaAttribute("ratio")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("ratio")
		public BigDecimal getRatio() {
			return ratio;
		}
		
		@Override
		@RosettaAttribute("code")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("code")
		public String getCode() {
			return code;
		}
		
		@Override
		public C1Leaf build() {
			return this;
		}
		
		@Override
		public C1Leaf.C1LeafBuilder toBuilder() {
			C1Leaf.C1LeafBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C1Leaf.C1LeafBuilder builder) {
			super.setBuilderFields(builder);
			ofNullable(getNote()).ifPresent(builder::setNoteOverriddenAsString);
			ofNullable(getParts()).ifPresent(builder::setParts);
			ofNullable(getRatio()).ifPresent(builder::setRatio);
			ofNullable(getCode()).ifPresent(builder::setCode);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
			C1Leaf _that = getType().cast(o);
		
			if (!Objects.equals(note, _that.getNote())) return false;
			if (!ListEquals.listEquals(parts, _that.getParts())) return false;
			if (!Objects.equals(ratio, _that.getRatio())) return false;
			if (!Objects.equals(code, _that.getCode())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			_result = 31 * _result + (note != null ? note.hashCode() : 0);
			_result = 31 * _result + (parts != null ? parts.hashCode() : 0);
			_result = 31 * _result + (ratio != null ? ratio.hashCode() : 0);
			_result = 31 * _result + (code != null ? code.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C1Leaf {" +
				"note=" + this.note + ", " +
				"parts=" + this.parts + ", " +
				"ratio=" + this.ratio + ", " +
				"code=" + this.code +
			'}' + " " + super.toString();
		}
	}

	/*********************** Builder Implementation of C1Leaf  ***********************/
	class C1LeafBuilderImpl extends C1Mid.C1MidBuilderImpl implements C1Leaf.C1LeafBuilder {
	
		protected String note;
		protected List<C1Ref.C1RefBuilder> parts = new ArrayList<>();
		protected BigDecimal ratio;
		protected String code;
		
		@Override
		@RosettaAttribute("note")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("note")
		public String getNote() {
			return note;
		}
		
		@Override
		@RosettaAttribute("parts")
		@Accessor(AccessorType.GETTER)
		@Required
		@Multi
		@RuneAttribute("parts")
		public List<? extends C1Ref.C1RefBuilder> getParts() {
			return parts;
		}
		
		@Override
		public C1Ref.C1RefBuilder getOrCreateParts(int index) {
			if (parts==null) {
				this.parts = new ArrayList<>();
			}
			return getIndex(parts, index, () -> {
						C1Ref.C1RefBuilder newParts = C1Ref.builder();
						return newParts;
					});
		}
		
		@Override
		@RosettaAttribute("ratio")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("ratio")
		public BigDecimal getRatio() {
			return ratio;
		}
		
		@Override
		@RosettaAttribute("code")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("code")
		public String getCode() {
			return code;
		}
		
		@RosettaAttribute("baseId")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("baseId")
		@Override
		public C1Leaf.C1LeafBuilder setBaseId(String _baseId) {
			this.baseId = _baseId == null ? null : _baseId;
			return this;
		}
		
		@RosettaAttribute("note")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("note")
		@Override
		public C1Leaf.C1LeafBuilder setNoteOverriddenAsString(String _note) {
			this.note = _note == null ? null : _note;
			return this;
		}
		
		@RosettaIgnore
		@RuneIgnore
		@Override
		public C1Leaf.C1LeafBuilder setNote(String _note) {
			return setNoteOverriddenAsString(_note);
		}
		
		@RosettaAttribute("mids")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("mids")
		@Override
		public C1Leaf.C1LeafBuilder addMids(BigDecimal _mids) {
			if (_mids != null) {
				this.mids.add(_mids);
			}
			return this;
		}
		
		@Override
		public C1Leaf.C1LeafBuilder addMids(BigDecimal _mids, int idx) {
			getIndex(this.mids, idx, () -> _mids);
			return this;
		}
		
		@Override
		public C1Leaf.C1LeafBuilder addMids(List<BigDecimal> midss) {
			if (midss != null) {
				for (final BigDecimal toAdd : midss) {
					this.mids.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("mids")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("mids")
		@Override
		public C1Leaf.C1LeafBuilder setMids(List<BigDecimal> midss) {
			if (midss == null) {
				this.mids = new ArrayList<>();
			} else {
				this.mids = midss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("asOf")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("asOf")
		@Override
		public C1Leaf.C1LeafBuilder setAsOf(Date _asOf) {
			this.asOf = _asOf == null ? null : _asOf;
			return this;
		}
		
		@RosettaAttribute("parts")
		@Accessor(AccessorType.ADDER)
		@Required
		@Multi
		@RuneAttribute("parts")
		@Override
		public C1Leaf.C1LeafBuilder addParts(C1Ref _parts) {
			if (_parts != null) {
				this.parts.add(_parts.toBuilder());
			}
			return this;
		}
		
		@Override
		public C1Leaf.C1LeafBuilder addParts(C1Ref _parts, int idx) {
			getIndex(this.parts, idx, () -> _parts.toBuilder());
			return this;
		}
		
		@Override
		public C1Leaf.C1LeafBuilder addParts(List<? extends C1Ref> partss) {
			if (partss != null) {
				for (final C1Ref toAdd : partss) {
					this.parts.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("parts")
		@Accessor(AccessorType.SETTER)
		@Required
		@Multi
		@RuneAttribute("parts")
		@Override
		public C1Leaf.C1LeafBuilder setParts(List<? extends C1Ref> partss) {
			if (partss == null) {
				this.parts = new ArrayList<>();
			} else {
				this.parts = partss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("ratio")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("ratio")
		@Override
		public C1Leaf.C1LeafBuilder setRatio(BigDecimal _ratio) {
			this.ratio = _ratio == null ? null : _ratio;
			return this;
		}
		
		@RosettaAttribute("code")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("code")
		@Override
		public C1Leaf.C1LeafBuilder setCode(String _code) {
			this.code = _code == null ? null : _code;
			return this;
		}
		
		@Override
		public C1Leaf build() {
			return new C1Leaf.C1LeafImpl(this);
		}
		
		@Override
		public C1Leaf.C1LeafBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C1Leaf.C1LeafBuilder prune() {
			super.prune();
			parts = parts.stream().filter(b->b!=null).<C1Ref.C1RefBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (super.hasData()) return true;
			if (getNote()!=null) return true;
			if (getParts()!=null && getParts().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			if (getRatio()!=null) return true;
			if (getCode()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C1Leaf.C1LeafBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			super.merge(other, merger);
			C1Leaf.C1LeafBuilder o = (C1Leaf.C1LeafBuilder) other;
			
			merger.mergeRosetta(getParts(), o.getParts(), this::getOrCreateParts);
			
			merger.mergeBasic(getNote(), o.getNote(), this::setNoteOverriddenAsString);
			merger.mergeBasic(getRatio(), o.getRatio(), this::setRatio);
			merger.mergeBasic(getCode(), o.getCode(), this::setCode);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
			C1Leaf _that = getType().cast(o);
		
			if (!Objects.equals(note, _that.getNote())) return false;
			if (!ListEquals.listEquals(parts, _that.getParts())) return false;
			if (!Objects.equals(ratio, _that.getRatio())) return false;
			if (!Objects.equals(code, _that.getCode())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			_result = 31 * _result + (note != null ? note.hashCode() : 0);
			_result = 31 * _result + (parts != null ? parts.hashCode() : 0);
			_result = 31 * _result + (ratio != null ? ratio.hashCode() : 0);
			_result = 31 * _result + (code != null ? code.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C1LeafBuilder {" +
				"note=" + this.note + ", " +
				"parts=" + this.parts + ", " +
				"ratio=" + this.ratio + ", " +
				"code=" + this.code +
			'}' + " " + super.toString();
		}
	}
}
