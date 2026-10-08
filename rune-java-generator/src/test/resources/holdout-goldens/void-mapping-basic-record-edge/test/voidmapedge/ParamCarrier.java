package test.voidmapedge;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;
import test.voidmapedge.meta.ParamCarrierMeta;

import static java.util.Optional.ofNullable;

/**
 * The parameterised basic type at an attribute.
 * @version 1.0.0
 */
@RosettaDataType(value="ParamCarrier", builder=ParamCarrier.ParamCarrierBuilderImpl.class, version="1.0.0")
@RuneDataType(value="ParamCarrier", model="test", builder=ParamCarrier.ParamCarrierBuilderImpl.class, version="1.0.0")
public interface ParamCarrier extends RosettaModelObject {

	ParamCarrierMeta metaData = new ParamCarrierMeta();

	/*********************** Getter Methods  ***********************/
	Void getScaled();

	/*********************** Build Methods  ***********************/
	ParamCarrier build();
	
	ParamCarrier.ParamCarrierBuilder toBuilder();
	
	static ParamCarrier.ParamCarrierBuilder builder() {
		return new ParamCarrier.ParamCarrierBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends ParamCarrier> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends ParamCarrier> getType() {
		return ParamCarrier.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("scaled"), Void.class, getScaled(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface ParamCarrierBuilder extends ParamCarrier, RosettaModelObjectBuilder {
		ParamCarrier.ParamCarrierBuilder setScaled(Void scaled);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("scaled"), Void.class, getScaled(), this);
		}
		

		ParamCarrier.ParamCarrierBuilder prune();
	}

	/*********************** Immutable Implementation of ParamCarrier  ***********************/
	class ParamCarrierImpl implements ParamCarrier {
		private final Void scaled;
		
		protected ParamCarrierImpl(ParamCarrier.ParamCarrierBuilder builder) {
			this.scaled = builder.getScaled();
		}
		
		@Override
		@RosettaAttribute("scaled")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("scaled")
		public Void getScaled() {
			return scaled;
		}
		
		@Override
		public ParamCarrier build() {
			return this;
		}
		
		@Override
		public ParamCarrier.ParamCarrierBuilder toBuilder() {
			ParamCarrier.ParamCarrierBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ParamCarrier.ParamCarrierBuilder builder) {
			ofNullable(getScaled()).ifPresent(builder::setScaled);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ParamCarrier _that = getType().cast(o);
		
			if (!Objects.equals(scaled, _that.getScaled())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (scaled != null ? scaled.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ParamCarrier {" +
				"scaled=" + this.scaled +
			'}';
		}
	}

	/*********************** Builder Implementation of ParamCarrier  ***********************/
	class ParamCarrierBuilderImpl implements ParamCarrier.ParamCarrierBuilder {
	
		protected Void scaled;
		
		@Override
		@RosettaAttribute("scaled")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("scaled")
		public Void getScaled() {
			return scaled;
		}
		
		@RosettaAttribute("scaled")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("scaled")
		@Override
		public ParamCarrier.ParamCarrierBuilder setScaled(Void _scaled) {
			this.scaled = _scaled == null ? null : _scaled;
			return this;
		}
		
		@Override
		public ParamCarrier build() {
			return new ParamCarrier.ParamCarrierImpl(this);
		}
		
		@Override
		public ParamCarrier.ParamCarrierBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ParamCarrier.ParamCarrierBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getScaled()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ParamCarrier.ParamCarrierBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ParamCarrier.ParamCarrierBuilder o = (ParamCarrier.ParamCarrierBuilder) other;
			
			
			merger.mergeBasic(getScaled(), o.getScaled(), this::setScaled);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ParamCarrier _that = getType().cast(o);
		
			if (!Objects.equals(scaled, _that.getScaled())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (scaled != null ? scaled.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ParamCarrierBuilder {" +
				"scaled=" + this.scaled +
			'}';
		}
	}
}
