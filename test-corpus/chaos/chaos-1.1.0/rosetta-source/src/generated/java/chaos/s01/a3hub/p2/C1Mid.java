package chaos.s01.a3hub.p2;

import chaos.s01.a3hub.p2.meta.C1MidMeta;
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
import com.rosetta.model.lib.records.Date;
import com.rosetta.util.ListEquals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Middle of the chain - adds a list and a date.
 * @version 1.0.0
 */
@RosettaDataType(value="C1Mid", builder=C1Mid.C1MidBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C1Mid", model="chaos", builder=C1Mid.C1MidBuilderImpl.class, version="1.0.0")
public interface C1Mid extends C1Base {

	C1MidMeta metaData = new C1MidMeta();

	/*********************** Getter Methods  ***********************/
	List<BigDecimal> getMids();
	Date getAsOf();

	/*********************** Build Methods  ***********************/
	C1Mid build();
	
	C1Mid.C1MidBuilder toBuilder();
	
	static C1Mid.C1MidBuilder builder() {
		return new C1Mid.C1MidBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C1Mid> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C1Mid> getType() {
		return C1Mid.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("baseId"), String.class, getBaseId(), this);
		processor.processBasic(path.newSubPath("note"), String.class, getNote(), this);
		processor.processBasic(path.newSubPath("mids"), BigDecimal.class, getMids(), this);
		processor.processBasic(path.newSubPath("asOf"), Date.class, getAsOf(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C1MidBuilder extends C1Mid, C1Base.C1BaseBuilder {
		@Override
		C1Mid.C1MidBuilder setBaseId(String baseId);
		@Override
		C1Mid.C1MidBuilder setNote(String note);
		C1Mid.C1MidBuilder addMids(BigDecimal mids);
		C1Mid.C1MidBuilder addMids(BigDecimal mids, int idx);
		C1Mid.C1MidBuilder addMids(List<BigDecimal> mids);
		C1Mid.C1MidBuilder setMids(List<BigDecimal> mids);
		C1Mid.C1MidBuilder setAsOf(Date asOf);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("baseId"), String.class, getBaseId(), this);
			processor.processBasic(path.newSubPath("note"), String.class, getNote(), this);
			processor.processBasic(path.newSubPath("mids"), BigDecimal.class, getMids(), this);
			processor.processBasic(path.newSubPath("asOf"), Date.class, getAsOf(), this);
		}
		

		C1Mid.C1MidBuilder prune();
	}

	/*********************** Immutable Implementation of C1Mid  ***********************/
	class C1MidImpl extends C1Base.C1BaseImpl implements C1Mid {
		private final List<BigDecimal> mids;
		private final Date asOf;
		
		protected C1MidImpl(C1Mid.C1MidBuilder builder) {
			super(builder);
			this.mids = ofNullable(builder.getMids()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.asOf = builder.getAsOf();
		}
		
		@Override
		@RosettaAttribute("mids")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("mids")
		public List<BigDecimal> getMids() {
			return mids;
		}
		
		@Override
		@RosettaAttribute("asOf")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("asOf")
		public Date getAsOf() {
			return asOf;
		}
		
		@Override
		public C1Mid build() {
			return this;
		}
		
		@Override
		public C1Mid.C1MidBuilder toBuilder() {
			C1Mid.C1MidBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C1Mid.C1MidBuilder builder) {
			super.setBuilderFields(builder);
			ofNullable(getMids()).ifPresent(builder::setMids);
			ofNullable(getAsOf()).ifPresent(builder::setAsOf);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
			C1Mid _that = getType().cast(o);
		
			if (!ListEquals.listEquals(mids, _that.getMids())) return false;
			if (!Objects.equals(asOf, _that.getAsOf())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			_result = 31 * _result + (mids != null ? mids.hashCode() : 0);
			_result = 31 * _result + (asOf != null ? asOf.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C1Mid {" +
				"mids=" + this.mids + ", " +
				"asOf=" + this.asOf +
			'}' + " " + super.toString();
		}
	}

	/*********************** Builder Implementation of C1Mid  ***********************/
	class C1MidBuilderImpl extends C1Base.C1BaseBuilderImpl implements C1Mid.C1MidBuilder {
	
		protected List<BigDecimal> mids = new ArrayList<>();
		protected Date asOf;
		
		@Override
		@RosettaAttribute("mids")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("mids")
		public List<BigDecimal> getMids() {
			return mids;
		}
		
		@Override
		@RosettaAttribute("asOf")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("asOf")
		public Date getAsOf() {
			return asOf;
		}
		
		@RosettaAttribute("baseId")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("baseId")
		@Override
		public C1Mid.C1MidBuilder setBaseId(String _baseId) {
			this.baseId = _baseId == null ? null : _baseId;
			return this;
		}
		
		@RosettaAttribute("note")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("note")
		@Override
		public C1Mid.C1MidBuilder setNote(String _note) {
			this.note = _note == null ? null : _note;
			return this;
		}
		
		@RosettaAttribute("mids")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("mids")
		@Override
		public C1Mid.C1MidBuilder addMids(BigDecimal _mids) {
			if (_mids != null) {
				this.mids.add(_mids);
			}
			return this;
		}
		
		@Override
		public C1Mid.C1MidBuilder addMids(BigDecimal _mids, int idx) {
			getIndex(this.mids, idx, () -> _mids);
			return this;
		}
		
		@Override
		public C1Mid.C1MidBuilder addMids(List<BigDecimal> midss) {
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
		public C1Mid.C1MidBuilder setMids(List<BigDecimal> midss) {
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
		public C1Mid.C1MidBuilder setAsOf(Date _asOf) {
			this.asOf = _asOf == null ? null : _asOf;
			return this;
		}
		
		@Override
		public C1Mid build() {
			return new C1Mid.C1MidImpl(this);
		}
		
		@Override
		public C1Mid.C1MidBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C1Mid.C1MidBuilder prune() {
			super.prune();
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (super.hasData()) return true;
			if (getMids()!=null && !getMids().isEmpty()) return true;
			if (getAsOf()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C1Mid.C1MidBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			super.merge(other, merger);
			C1Mid.C1MidBuilder o = (C1Mid.C1MidBuilder) other;
			
			
			merger.mergeBasic(getMids(), o.getMids(), (Consumer<BigDecimal>) this::addMids);
			merger.mergeBasic(getAsOf(), o.getAsOf(), this::setAsOf);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
			C1Mid _that = getType().cast(o);
		
			if (!ListEquals.listEquals(mids, _that.getMids())) return false;
			if (!Objects.equals(asOf, _that.getAsOf())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			_result = 31 * _result + (mids != null ? mids.hashCode() : 0);
			_result = 31 * _result + (asOf != null ? asOf.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C1MidBuilder {" +
				"mids=" + this.mids + ", " +
				"asOf=" + this.asOf +
			'}' + " " + super.toString();
		}
	}
}
