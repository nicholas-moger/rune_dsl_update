package chaos.s23.a3hub.p1;

import chaos.s23.a3hub.p1.meta.C23BoxMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
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
import java.math.BigDecimal;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Helper - relocated by the import axis.
 * @version 1.0.0
 */
@RosettaDataType(value="C23Box", builder=C23Box.C23BoxBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C23Box", model="chaos", builder=C23Box.C23BoxBuilderImpl.class, version="1.0.0")
public interface C23Box extends RosettaModelObject {

	C23BoxMeta metaData = new C23BoxMeta();

	/*********************** Getter Methods  ***********************/
	String getLid();
	BigDecimal getWeight();
	Boolean getLive();

	/*********************** Build Methods  ***********************/
	C23Box build();
	
	C23Box.C23BoxBuilder toBuilder();
	
	static C23Box.C23BoxBuilder builder() {
		return new C23Box.C23BoxBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C23Box> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C23Box> getType() {
		return C23Box.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("lid"), String.class, getLid(), this);
		processor.processBasic(path.newSubPath("weight"), BigDecimal.class, getWeight(), this);
		processor.processBasic(path.newSubPath("live"), Boolean.class, getLive(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C23BoxBuilder extends C23Box, RosettaModelObjectBuilder {
		C23Box.C23BoxBuilder setLid(String lid);
		C23Box.C23BoxBuilder setWeight(BigDecimal weight);
		C23Box.C23BoxBuilder setLive(Boolean live);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("lid"), String.class, getLid(), this);
			processor.processBasic(path.newSubPath("weight"), BigDecimal.class, getWeight(), this);
			processor.processBasic(path.newSubPath("live"), Boolean.class, getLive(), this);
		}
		

		C23Box.C23BoxBuilder prune();
	}

	/*********************** Immutable Implementation of C23Box  ***********************/
	class C23BoxImpl implements C23Box {
		private final String lid;
		private final BigDecimal weight;
		private final Boolean live;
		
		protected C23BoxImpl(C23Box.C23BoxBuilder builder) {
			this.lid = builder.getLid();
			this.weight = builder.getWeight();
			this.live = builder.getLive();
		}
		
		@Override
		@RosettaAttribute("lid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("lid")
		public String getLid() {
			return lid;
		}
		
		@Override
		@RosettaAttribute("weight")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("weight")
		public BigDecimal getWeight() {
			return weight;
		}
		
		@Override
		@RosettaAttribute("live")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("live")
		public Boolean getLive() {
			return live;
		}
		
		@Override
		public C23Box build() {
			return this;
		}
		
		@Override
		public C23Box.C23BoxBuilder toBuilder() {
			C23Box.C23BoxBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C23Box.C23BoxBuilder builder) {
			ofNullable(getLid()).ifPresent(builder::setLid);
			ofNullable(getWeight()).ifPresent(builder::setWeight);
			ofNullable(getLive()).ifPresent(builder::setLive);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C23Box _that = getType().cast(o);
		
			if (!Objects.equals(lid, _that.getLid())) return false;
			if (!Objects.equals(weight, _that.getWeight())) return false;
			if (!Objects.equals(live, _that.getLive())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (lid != null ? lid.hashCode() : 0);
			_result = 31 * _result + (weight != null ? weight.hashCode() : 0);
			_result = 31 * _result + (live != null ? live.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C23Box {" +
				"lid=" + this.lid + ", " +
				"weight=" + this.weight + ", " +
				"live=" + this.live +
			'}';
		}
	}

	/*********************** Builder Implementation of C23Box  ***********************/
	class C23BoxBuilderImpl implements C23Box.C23BoxBuilder {
	
		protected String lid;
		protected BigDecimal weight;
		protected Boolean live;
		
		@Override
		@RosettaAttribute("lid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("lid")
		public String getLid() {
			return lid;
		}
		
		@Override
		@RosettaAttribute("weight")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("weight")
		public BigDecimal getWeight() {
			return weight;
		}
		
		@Override
		@RosettaAttribute("live")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("live")
		public Boolean getLive() {
			return live;
		}
		
		@RosettaAttribute("lid")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("lid")
		@Override
		public C23Box.C23BoxBuilder setLid(String _lid) {
			this.lid = _lid == null ? null : _lid;
			return this;
		}
		
		@RosettaAttribute("weight")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("weight")
		@Override
		public C23Box.C23BoxBuilder setWeight(BigDecimal _weight) {
			this.weight = _weight == null ? null : _weight;
			return this;
		}
		
		@RosettaAttribute("live")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("live")
		@Override
		public C23Box.C23BoxBuilder setLive(Boolean _live) {
			this.live = _live == null ? null : _live;
			return this;
		}
		
		@Override
		public C23Box build() {
			return new C23Box.C23BoxImpl(this);
		}
		
		@Override
		public C23Box.C23BoxBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C23Box.C23BoxBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getLid()!=null) return true;
			if (getWeight()!=null) return true;
			if (getLive()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C23Box.C23BoxBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C23Box.C23BoxBuilder o = (C23Box.C23BoxBuilder) other;
			
			
			merger.mergeBasic(getLid(), o.getLid(), this::setLid);
			merger.mergeBasic(getWeight(), o.getWeight(), this::setWeight);
			merger.mergeBasic(getLive(), o.getLive(), this::setLive);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C23Box _that = getType().cast(o);
		
			if (!Objects.equals(lid, _that.getLid())) return false;
			if (!Objects.equals(weight, _that.getWeight())) return false;
			if (!Objects.equals(live, _that.getLive())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (lid != null ? lid.hashCode() : 0);
			_result = 31 * _result + (weight != null ? weight.hashCode() : 0);
			_result = 31 * _result + (live != null ? live.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C23BoxBuilder {" +
				"lid=" + this.lid + ", " +
				"weight=" + this.weight + ", " +
				"live=" + this.live +
			'}';
		}
	}
}
