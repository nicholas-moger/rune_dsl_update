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
import test.voidmapedge.meta.ConditionCarrierMeta;

import static java.util.Optional.ofNullable;

/**
 * An expression over a Void-typed attribute.
 * @version 1.0.0
 */
@RosettaDataType(value="ConditionCarrier", builder=ConditionCarrier.ConditionCarrierBuilderImpl.class, version="1.0.0")
@RuneDataType(value="ConditionCarrier", model="test", builder=ConditionCarrier.ConditionCarrierBuilderImpl.class, version="1.0.0")
public interface ConditionCarrier extends RosettaModelObject {

	ConditionCarrierMeta metaData = new ConditionCarrierMeta();

	/*********************** Getter Methods  ***********************/
	Void getTok();
	Boolean getFlag();

	/*********************** Build Methods  ***********************/
	ConditionCarrier build();
	
	ConditionCarrier.ConditionCarrierBuilder toBuilder();
	
	static ConditionCarrier.ConditionCarrierBuilder builder() {
		return new ConditionCarrier.ConditionCarrierBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends ConditionCarrier> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends ConditionCarrier> getType() {
		return ConditionCarrier.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("tok"), Void.class, getTok(), this);
		processor.processBasic(path.newSubPath("flag"), Boolean.class, getFlag(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface ConditionCarrierBuilder extends ConditionCarrier, RosettaModelObjectBuilder {
		ConditionCarrier.ConditionCarrierBuilder setTok(Void tok);
		ConditionCarrier.ConditionCarrierBuilder setFlag(Boolean flag);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("tok"), Void.class, getTok(), this);
			processor.processBasic(path.newSubPath("flag"), Boolean.class, getFlag(), this);
		}
		

		ConditionCarrier.ConditionCarrierBuilder prune();
	}

	/*********************** Immutable Implementation of ConditionCarrier  ***********************/
	class ConditionCarrierImpl implements ConditionCarrier {
		private final Void tok;
		private final Boolean flag;
		
		protected ConditionCarrierImpl(ConditionCarrier.ConditionCarrierBuilder builder) {
			this.tok = builder.getTok();
			this.flag = builder.getFlag();
		}
		
		@Override
		@RosettaAttribute("tok")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tok")
		public Void getTok() {
			return tok;
		}
		
		@Override
		@RosettaAttribute("flag")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("flag")
		public Boolean getFlag() {
			return flag;
		}
		
		@Override
		public ConditionCarrier build() {
			return this;
		}
		
		@Override
		public ConditionCarrier.ConditionCarrierBuilder toBuilder() {
			ConditionCarrier.ConditionCarrierBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ConditionCarrier.ConditionCarrierBuilder builder) {
			ofNullable(getTok()).ifPresent(builder::setTok);
			ofNullable(getFlag()).ifPresent(builder::setFlag);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ConditionCarrier _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			if (!Objects.equals(flag, _that.getFlag())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			_result = 31 * _result + (flag != null ? flag.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ConditionCarrier {" +
				"tok=" + this.tok + ", " +
				"flag=" + this.flag +
			'}';
		}
	}

	/*********************** Builder Implementation of ConditionCarrier  ***********************/
	class ConditionCarrierBuilderImpl implements ConditionCarrier.ConditionCarrierBuilder {
	
		protected Void tok;
		protected Boolean flag;
		
		@Override
		@RosettaAttribute("tok")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tok")
		public Void getTok() {
			return tok;
		}
		
		@Override
		@RosettaAttribute("flag")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("flag")
		public Boolean getFlag() {
			return flag;
		}
		
		@RosettaAttribute("tok")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("tok")
		@Override
		public ConditionCarrier.ConditionCarrierBuilder setTok(Void _tok) {
			this.tok = _tok == null ? null : _tok;
			return this;
		}
		
		@RosettaAttribute("flag")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("flag")
		@Override
		public ConditionCarrier.ConditionCarrierBuilder setFlag(Boolean _flag) {
			this.flag = _flag == null ? null : _flag;
			return this;
		}
		
		@Override
		public ConditionCarrier build() {
			return new ConditionCarrier.ConditionCarrierImpl(this);
		}
		
		@Override
		public ConditionCarrier.ConditionCarrierBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ConditionCarrier.ConditionCarrierBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getTok()!=null) return true;
			if (getFlag()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ConditionCarrier.ConditionCarrierBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ConditionCarrier.ConditionCarrierBuilder o = (ConditionCarrier.ConditionCarrierBuilder) other;
			
			
			merger.mergeBasic(getTok(), o.getTok(), this::setTok);
			merger.mergeBasic(getFlag(), o.getFlag(), this::setFlag);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ConditionCarrier _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			if (!Objects.equals(flag, _that.getFlag())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			_result = 31 * _result + (flag != null ? flag.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ConditionCarrierBuilder {" +
				"tok=" + this.tok + ", " +
				"flag=" + this.flag +
			'}';
		}
	}
}
