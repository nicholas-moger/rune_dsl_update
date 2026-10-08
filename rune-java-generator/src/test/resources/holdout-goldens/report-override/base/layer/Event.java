package base.layer;

import base.layer.meta.EventMeta;
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
@RosettaDataType(value="Event", builder=Event.EventBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Event", model="base", builder=Event.EventBuilderImpl.class, version="0.0.0")
public interface Event extends RosettaModelObject {

	EventMeta metaData = new EventMeta();

	/*********************** Getter Methods  ***********************/
	TradeEnum getTrade();

	/*********************** Build Methods  ***********************/
	Event build();
	
	Event.EventBuilder toBuilder();
	
	static Event.EventBuilder builder() {
		return new Event.EventBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Event> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Event> getType() {
		return Event.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("trade"), TradeEnum.class, getTrade(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface EventBuilder extends Event, RosettaModelObjectBuilder {
		Event.EventBuilder setTrade(TradeEnum trade);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("trade"), TradeEnum.class, getTrade(), this);
		}
		

		Event.EventBuilder prune();
	}

	/*********************** Immutable Implementation of Event  ***********************/
	class EventImpl implements Event {
		private final TradeEnum trade;
		
		protected EventImpl(Event.EventBuilder builder) {
			this.trade = builder.getTrade();
		}
		
		@Override
		@RosettaAttribute("trade")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("trade")
		public TradeEnum getTrade() {
			return trade;
		}
		
		@Override
		public Event build() {
			return this;
		}
		
		@Override
		public Event.EventBuilder toBuilder() {
			Event.EventBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Event.EventBuilder builder) {
			ofNullable(getTrade()).ifPresent(builder::setTrade);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Event _that = getType().cast(o);
		
			if (!Objects.equals(trade, _that.getTrade())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (trade != null ? trade.getClass().getName().hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Event {" +
				"trade=" + this.trade +
			'}';
		}
	}

	/*********************** Builder Implementation of Event  ***********************/
	class EventBuilderImpl implements Event.EventBuilder {
	
		protected TradeEnum trade;
		
		@Override
		@RosettaAttribute("trade")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("trade")
		public TradeEnum getTrade() {
			return trade;
		}
		
		@RosettaAttribute("trade")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("trade")
		@Override
		public Event.EventBuilder setTrade(TradeEnum _trade) {
			this.trade = _trade == null ? null : _trade;
			return this;
		}
		
		@Override
		public Event build() {
			return new Event.EventImpl(this);
		}
		
		@Override
		public Event.EventBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Event.EventBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getTrade()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Event.EventBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Event.EventBuilder o = (Event.EventBuilder) other;
			
			
			merger.mergeBasic(getTrade(), o.getTrade(), this::setTrade);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Event _that = getType().cast(o);
		
			if (!Objects.equals(trade, _that.getTrade())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (trade != null ? trade.getClass().getName().hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "EventBuilder {" +
				"trade=" + this.trade +
			'}';
		}
	}
}
