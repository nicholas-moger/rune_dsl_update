package test.voidmap;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import test.voidmap.meta.ListCarrierMeta;

import static java.util.Optional.ofNullable;

/**
 * List cardinality over the model-declared types.
 * @version 1.0.0
 */
@RosettaDataType(value="ListCarrier", builder=ListCarrier.ListCarrierBuilderImpl.class, version="1.0.0")
@RuneDataType(value="ListCarrier", model="test", builder=ListCarrier.ListCarrierBuilderImpl.class, version="1.0.0")
public interface ListCarrier extends RosettaModelObject {

	ListCarrierMeta metaData = new ListCarrierMeta();

	/*********************** Getter Methods  ***********************/
	List<Void> getToks();
	List<Void> getSpans();

	/*********************** Build Methods  ***********************/
	ListCarrier build();
	
	ListCarrier.ListCarrierBuilder toBuilder();
	
	static ListCarrier.ListCarrierBuilder builder() {
		return new ListCarrier.ListCarrierBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends ListCarrier> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends ListCarrier> getType() {
		return ListCarrier.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("toks"), Void.class, getToks(), this);
		processor.processBasic(path.newSubPath("spans"), Void.class, getSpans(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface ListCarrierBuilder extends ListCarrier, RosettaModelObjectBuilder {
		ListCarrier.ListCarrierBuilder addToks(Void toks);
		ListCarrier.ListCarrierBuilder addToks(Void toks, int idx);
		ListCarrier.ListCarrierBuilder addToks(List<Void> toks);
		ListCarrier.ListCarrierBuilder setToks(List<Void> toks);
		ListCarrier.ListCarrierBuilder addSpans(Void spans);
		ListCarrier.ListCarrierBuilder addSpans(Void spans, int idx);
		ListCarrier.ListCarrierBuilder addSpans(List<Void> spans);
		ListCarrier.ListCarrierBuilder setSpans(List<Void> spans);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("toks"), Void.class, getToks(), this);
			processor.processBasic(path.newSubPath("spans"), Void.class, getSpans(), this);
		}
		

		ListCarrier.ListCarrierBuilder prune();
	}

	/*********************** Immutable Implementation of ListCarrier  ***********************/
	class ListCarrierImpl implements ListCarrier {
		private final List<Void> toks;
		private final List<Void> spans;
		
		protected ListCarrierImpl(ListCarrier.ListCarrierBuilder builder) {
			this.toks = ofNullable(builder.getToks()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.spans = ofNullable(builder.getSpans()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("toks")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("toks")
		public List<Void> getToks() {
			return toks;
		}
		
		@Override
		@RosettaAttribute("spans")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("spans")
		public List<Void> getSpans() {
			return spans;
		}
		
		@Override
		public ListCarrier build() {
			return this;
		}
		
		@Override
		public ListCarrier.ListCarrierBuilder toBuilder() {
			ListCarrier.ListCarrierBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ListCarrier.ListCarrierBuilder builder) {
			ofNullable(getToks()).ifPresent(builder::setToks);
			ofNullable(getSpans()).ifPresent(builder::setSpans);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ListCarrier _that = getType().cast(o);
		
			if (!ListEquals.listEquals(toks, _that.getToks())) return false;
			if (!ListEquals.listEquals(spans, _that.getSpans())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (toks != null ? toks.hashCode() : 0);
			_result = 31 * _result + (spans != null ? spans.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ListCarrier {" +
				"toks=" + this.toks + ", " +
				"spans=" + this.spans +
			'}';
		}
	}

	/*********************** Builder Implementation of ListCarrier  ***********************/
	class ListCarrierBuilderImpl implements ListCarrier.ListCarrierBuilder {
	
		protected List<Void> toks = new ArrayList<>();
		protected List<Void> spans = new ArrayList<>();
		
		@Override
		@RosettaAttribute("toks")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("toks")
		public List<Void> getToks() {
			return toks;
		}
		
		@Override
		@RosettaAttribute("spans")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("spans")
		public List<Void> getSpans() {
			return spans;
		}
		
		@RosettaAttribute("toks")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("toks")
		@Override
		public ListCarrier.ListCarrierBuilder addToks(Void _toks) {
			if (_toks != null) {
				this.toks.add(_toks);
			}
			return this;
		}
		
		@Override
		public ListCarrier.ListCarrierBuilder addToks(Void _toks, int idx) {
			getIndex(this.toks, idx, () -> _toks);
			return this;
		}
		
		@Override
		public ListCarrier.ListCarrierBuilder addToks(List<Void> tokss) {
			if (tokss != null) {
				for (final Void toAdd : tokss) {
					this.toks.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("toks")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("toks")
		@Override
		public ListCarrier.ListCarrierBuilder setToks(List<Void> tokss) {
			if (tokss == null) {
				this.toks = new ArrayList<>();
			} else {
				this.toks = tokss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("spans")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("spans")
		@Override
		public ListCarrier.ListCarrierBuilder addSpans(Void _spans) {
			if (_spans != null) {
				this.spans.add(_spans);
			}
			return this;
		}
		
		@Override
		public ListCarrier.ListCarrierBuilder addSpans(Void _spans, int idx) {
			getIndex(this.spans, idx, () -> _spans);
			return this;
		}
		
		@Override
		public ListCarrier.ListCarrierBuilder addSpans(List<Void> spanss) {
			if (spanss != null) {
				for (final Void toAdd : spanss) {
					this.spans.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("spans")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("spans")
		@Override
		public ListCarrier.ListCarrierBuilder setSpans(List<Void> spanss) {
			if (spanss == null) {
				this.spans = new ArrayList<>();
			} else {
				this.spans = spanss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public ListCarrier build() {
			return new ListCarrier.ListCarrierImpl(this);
		}
		
		@Override
		public ListCarrier.ListCarrierBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ListCarrier.ListCarrierBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getToks()!=null && !getToks().isEmpty()) return true;
			if (getSpans()!=null && !getSpans().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ListCarrier.ListCarrierBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ListCarrier.ListCarrierBuilder o = (ListCarrier.ListCarrierBuilder) other;
			
			
			merger.mergeBasic(getToks(), o.getToks(), (Consumer<Void>) this::addToks);
			merger.mergeBasic(getSpans(), o.getSpans(), (Consumer<Void>) this::addSpans);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ListCarrier _that = getType().cast(o);
		
			if (!ListEquals.listEquals(toks, _that.getToks())) return false;
			if (!ListEquals.listEquals(spans, _that.getSpans())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (toks != null ? toks.hashCode() : 0);
			_result = 31 * _result + (spans != null ? spans.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ListCarrierBuilder {" +
				"toks=" + this.toks + ", " +
				"spans=" + this.spans +
			'}';
		}
	}
}
