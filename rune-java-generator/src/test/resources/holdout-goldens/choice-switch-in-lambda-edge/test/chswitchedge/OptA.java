package test.chswitchedge;

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
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import test.chswitchedge.meta.OptAMeta;

import static java.util.Optional.ofNullable;

/**
 * Choice option A (the chaos C18OptA, widened by tags).
 * @version 0.0.0
 */
@RosettaDataType(value="OptA", builder=OptA.OptABuilderImpl.class, version="0.0.0")
@RuneDataType(value="OptA", model="test", builder=OptA.OptABuilderImpl.class, version="0.0.0")
public interface OptA extends RosettaModelObject {

	OptAMeta metaData = new OptAMeta();

	/*********************** Getter Methods  ***********************/
	String getAv();
	List<String> getTags();

	/*********************** Build Methods  ***********************/
	OptA build();
	
	OptA.OptABuilder toBuilder();
	
	static OptA.OptABuilder builder() {
		return new OptA.OptABuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends OptA> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends OptA> getType() {
		return OptA.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("av"), String.class, getAv(), this);
		processor.processBasic(path.newSubPath("tags"), String.class, getTags(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface OptABuilder extends OptA, RosettaModelObjectBuilder {
		OptA.OptABuilder setAv(String av);
		OptA.OptABuilder addTags(String tags);
		OptA.OptABuilder addTags(String tags, int idx);
		OptA.OptABuilder addTags(List<String> tags);
		OptA.OptABuilder setTags(List<String> tags);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("av"), String.class, getAv(), this);
			processor.processBasic(path.newSubPath("tags"), String.class, getTags(), this);
		}
		

		OptA.OptABuilder prune();
	}

	/*********************** Immutable Implementation of OptA  ***********************/
	class OptAImpl implements OptA {
		private final String av;
		private final List<String> tags;
		
		protected OptAImpl(OptA.OptABuilder builder) {
			this.av = builder.getAv();
			this.tags = ofNullable(builder.getTags()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("av")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("av")
		public String getAv() {
			return av;
		}
		
		@Override
		@RosettaAttribute("tags")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("tags")
		public List<String> getTags() {
			return tags;
		}
		
		@Override
		public OptA build() {
			return this;
		}
		
		@Override
		public OptA.OptABuilder toBuilder() {
			OptA.OptABuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(OptA.OptABuilder builder) {
			ofNullable(getAv()).ifPresent(builder::setAv);
			ofNullable(getTags()).ifPresent(builder::setTags);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			OptA _that = getType().cast(o);
		
			if (!Objects.equals(av, _that.getAv())) return false;
			if (!ListEquals.listEquals(tags, _that.getTags())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (av != null ? av.hashCode() : 0);
			_result = 31 * _result + (tags != null ? tags.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OptA {" +
				"av=" + this.av + ", " +
				"tags=" + this.tags +
			'}';
		}
	}

	/*********************** Builder Implementation of OptA  ***********************/
	class OptABuilderImpl implements OptA.OptABuilder {
	
		protected String av;
		protected List<String> tags = new ArrayList<>();
		
		@Override
		@RosettaAttribute("av")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("av")
		public String getAv() {
			return av;
		}
		
		@Override
		@RosettaAttribute("tags")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("tags")
		public List<String> getTags() {
			return tags;
		}
		
		@RosettaAttribute("av")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("av")
		@Override
		public OptA.OptABuilder setAv(String _av) {
			this.av = _av == null ? null : _av;
			return this;
		}
		
		@RosettaAttribute("tags")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("tags")
		@Override
		public OptA.OptABuilder addTags(String _tags) {
			if (_tags != null) {
				this.tags.add(_tags);
			}
			return this;
		}
		
		@Override
		public OptA.OptABuilder addTags(String _tags, int idx) {
			getIndex(this.tags, idx, () -> _tags);
			return this;
		}
		
		@Override
		public OptA.OptABuilder addTags(List<String> tagss) {
			if (tagss != null) {
				for (final String toAdd : tagss) {
					this.tags.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("tags")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("tags")
		@Override
		public OptA.OptABuilder setTags(List<String> tagss) {
			if (tagss == null) {
				this.tags = new ArrayList<>();
			} else {
				this.tags = tagss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public OptA build() {
			return new OptA.OptAImpl(this);
		}
		
		@Override
		public OptA.OptABuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public OptA.OptABuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAv()!=null) return true;
			if (getTags()!=null && !getTags().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public OptA.OptABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			OptA.OptABuilder o = (OptA.OptABuilder) other;
			
			
			merger.mergeBasic(getAv(), o.getAv(), this::setAv);
			merger.mergeBasic(getTags(), o.getTags(), (Consumer<String>) this::addTags);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			OptA _that = getType().cast(o);
		
			if (!Objects.equals(av, _that.getAv())) return false;
			if (!ListEquals.listEquals(tags, _that.getTags())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (av != null ? av.hashCode() : 0);
			_result = 31 * _result + (tags != null ? tags.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OptABuilder {" +
				"av=" + this.av + ", " +
				"tags=" + this.tags +
			'}';
		}
	}
}
