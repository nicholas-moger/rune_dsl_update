package base.layer;

import base.layer.meta.InstructionMeta;
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
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Instruction", builder=Instruction.InstructionBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Instruction", model="base", builder=Instruction.InstructionBuilderImpl.class, version="0.0.0")
public interface Instruction extends Event {

	InstructionMeta metaData = new InstructionMeta();

	/*********************** Getter Methods  ***********************/
	Integer getId();

	/*********************** Build Methods  ***********************/
	Instruction build();
	
	Instruction.InstructionBuilder toBuilder();
	
	static Instruction.InstructionBuilder builder() {
		return new Instruction.InstructionBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Instruction> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Instruction> getType() {
		return Instruction.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("trade"), TradeEnum.class, getTrade(), this);
		processor.processBasic(path.newSubPath("id"), Integer.class, getId(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface InstructionBuilder extends Instruction, Event.EventBuilder {
		@Override
		Instruction.InstructionBuilder setTrade(TradeEnum trade);
		Instruction.InstructionBuilder setId(Integer id);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("trade"), TradeEnum.class, getTrade(), this);
			processor.processBasic(path.newSubPath("id"), Integer.class, getId(), this);
		}
		

		Instruction.InstructionBuilder prune();
	}

	/*********************** Immutable Implementation of Instruction  ***********************/
	class InstructionImpl extends Event.EventImpl implements Instruction {
		private final Integer id;
		
		protected InstructionImpl(Instruction.InstructionBuilder builder) {
			super(builder);
			this.id = builder.getId();
		}
		
		@Override
		@RosettaAttribute("id")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("id")
		public Integer getId() {
			return id;
		}
		
		@Override
		public Instruction build() {
			return this;
		}
		
		@Override
		public Instruction.InstructionBuilder toBuilder() {
			Instruction.InstructionBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Instruction.InstructionBuilder builder) {
			super.setBuilderFields(builder);
			ofNullable(getId()).ifPresent(builder::setId);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
			Instruction _that = getType().cast(o);
		
			if (!Objects.equals(id, _that.getId())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			_result = 31 * _result + (id != null ? id.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Instruction {" +
				"id=" + this.id +
			'}' + " " + super.toString();
		}
	}

	/*********************** Builder Implementation of Instruction  ***********************/
	class InstructionBuilderImpl extends Event.EventBuilderImpl implements Instruction.InstructionBuilder {
	
		protected Integer id;
		
		@Override
		@RosettaAttribute("id")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("id")
		public Integer getId() {
			return id;
		}
		
		@RosettaAttribute("trade")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("trade")
		@Override
		public Instruction.InstructionBuilder setTrade(TradeEnum _trade) {
			this.trade = _trade == null ? null : _trade;
			return this;
		}
		
		@RosettaAttribute("id")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("id")
		@Override
		public Instruction.InstructionBuilder setId(Integer _id) {
			this.id = _id == null ? null : _id;
			return this;
		}
		
		@Override
		public Instruction build() {
			return new Instruction.InstructionImpl(this);
		}
		
		@Override
		public Instruction.InstructionBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Instruction.InstructionBuilder prune() {
			super.prune();
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (super.hasData()) return true;
			if (getId()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Instruction.InstructionBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			super.merge(other, merger);
			Instruction.InstructionBuilder o = (Instruction.InstructionBuilder) other;
			
			
			merger.mergeBasic(getId(), o.getId(), this::setId);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
			Instruction _that = getType().cast(o);
		
			if (!Objects.equals(id, _that.getId())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			_result = 31 * _result + (id != null ? id.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "InstructionBuilder {" +
				"id=" + this.id +
			'}' + " " + super.toString();
		}
	}
}
