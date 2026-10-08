package test.deeppath;

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
import test.deeppath.meta.WrapMeta;

import static java.util.Optional.ofNullable;

/**
 * Choice option 2 - wraps the inner choice; shares &#39;text&#39; and &#39;tags&#39; with Note (the chaos C3Wrap).
 * @version 0.0.0
 */
@RosettaDataType(value="Wrap", builder=Wrap.WrapBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Wrap", model="test", builder=Wrap.WrapBuilderImpl.class, version="0.0.0")
public interface Wrap extends RosettaModelObject {

	WrapMeta metaData = new WrapMeta();

	/*********************** Getter Methods  ***********************/
	Inner getInner();
	String getText();
	List<String> getTags();

	/*********************** Build Methods  ***********************/
	Wrap build();
	
	Wrap.WrapBuilder toBuilder();
	
	static Wrap.WrapBuilder builder() {
		return new Wrap.WrapBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Wrap> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Wrap> getType() {
		return Wrap.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("inner"), processor, Inner.class, getInner());
		processor.processBasic(path.newSubPath("text"), String.class, getText(), this);
		processor.processBasic(path.newSubPath("tags"), String.class, getTags(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface WrapBuilder extends Wrap, RosettaModelObjectBuilder {
		Inner.InnerBuilder getOrCreateInner();
		@Override
		Inner.InnerBuilder getInner();
		Wrap.WrapBuilder setInner(Inner inner);
		Wrap.WrapBuilder setText(String text);
		Wrap.WrapBuilder addTags(String tags);
		Wrap.WrapBuilder addTags(String tags, int idx);
		Wrap.WrapBuilder addTags(List<String> tags);
		Wrap.WrapBuilder setTags(List<String> tags);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("inner"), processor, Inner.InnerBuilder.class, getInner());
			processor.processBasic(path.newSubPath("text"), String.class, getText(), this);
			processor.processBasic(path.newSubPath("tags"), String.class, getTags(), this);
		}
		

		Wrap.WrapBuilder prune();
	}

	/*********************** Immutable Implementation of Wrap  ***********************/
	class WrapImpl implements Wrap {
		private final Inner inner;
		private final String text;
		private final List<String> tags;
		
		protected WrapImpl(Wrap.WrapBuilder builder) {
			this.inner = ofNullable(builder.getInner()).map(f->f.build()).orElse(null);
			this.text = builder.getText();
			this.tags = ofNullable(builder.getTags()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("inner")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("inner")
		public Inner getInner() {
			return inner;
		}
		
		@Override
		@RosettaAttribute("text")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("text")
		public String getText() {
			return text;
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
		public Wrap build() {
			return this;
		}
		
		@Override
		public Wrap.WrapBuilder toBuilder() {
			Wrap.WrapBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Wrap.WrapBuilder builder) {
			ofNullable(getInner()).ifPresent(builder::setInner);
			ofNullable(getText()).ifPresent(builder::setText);
			ofNullable(getTags()).ifPresent(builder::setTags);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Wrap _that = getType().cast(o);
		
			if (!Objects.equals(inner, _that.getInner())) return false;
			if (!Objects.equals(text, _that.getText())) return false;
			if (!ListEquals.listEquals(tags, _that.getTags())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (inner != null ? inner.hashCode() : 0);
			_result = 31 * _result + (text != null ? text.hashCode() : 0);
			_result = 31 * _result + (tags != null ? tags.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Wrap {" +
				"inner=" + this.inner + ", " +
				"text=" + this.text + ", " +
				"tags=" + this.tags +
			'}';
		}
	}

	/*********************** Builder Implementation of Wrap  ***********************/
	class WrapBuilderImpl implements Wrap.WrapBuilder {
	
		protected Inner.InnerBuilder inner;
		protected String text;
		protected List<String> tags = new ArrayList<>();
		
		@Override
		@RosettaAttribute("inner")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("inner")
		public Inner.InnerBuilder getInner() {
			return inner;
		}
		
		@Override
		public Inner.InnerBuilder getOrCreateInner() {
			Inner.InnerBuilder result;
			if (inner!=null) {
				result = inner;
			}
			else {
				result = inner = Inner.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("text")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("text")
		public String getText() {
			return text;
		}
		
		@Override
		@RosettaAttribute("tags")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("tags")
		public List<String> getTags() {
			return tags;
		}
		
		@RosettaAttribute("inner")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("inner")
		@Override
		public Wrap.WrapBuilder setInner(Inner _inner) {
			this.inner = _inner == null ? null : _inner.toBuilder();
			return this;
		}
		
		@RosettaAttribute("text")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("text")
		@Override
		public Wrap.WrapBuilder setText(String _text) {
			this.text = _text == null ? null : _text;
			return this;
		}
		
		@RosettaAttribute("tags")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("tags")
		@Override
		public Wrap.WrapBuilder addTags(String _tags) {
			if (_tags != null) {
				this.tags.add(_tags);
			}
			return this;
		}
		
		@Override
		public Wrap.WrapBuilder addTags(String _tags, int idx) {
			getIndex(this.tags, idx, () -> _tags);
			return this;
		}
		
		@Override
		public Wrap.WrapBuilder addTags(List<String> tagss) {
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
		public Wrap.WrapBuilder setTags(List<String> tagss) {
			if (tagss == null) {
				this.tags = new ArrayList<>();
			} else {
				this.tags = tagss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Wrap build() {
			return new Wrap.WrapImpl(this);
		}
		
		@Override
		public Wrap.WrapBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Wrap.WrapBuilder prune() {
			if (inner!=null && !inner.prune().hasData()) inner = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getInner()!=null && getInner().hasData()) return true;
			if (getText()!=null) return true;
			if (getTags()!=null && !getTags().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Wrap.WrapBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Wrap.WrapBuilder o = (Wrap.WrapBuilder) other;
			
			merger.mergeRosetta(getInner(), o.getInner(), this::setInner);
			
			merger.mergeBasic(getText(), o.getText(), this::setText);
			merger.mergeBasic(getTags(), o.getTags(), (Consumer<String>) this::addTags);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Wrap _that = getType().cast(o);
		
			if (!Objects.equals(inner, _that.getInner())) return false;
			if (!Objects.equals(text, _that.getText())) return false;
			if (!ListEquals.listEquals(tags, _that.getTags())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (inner != null ? inner.hashCode() : 0);
			_result = 31 * _result + (text != null ? text.hashCode() : 0);
			_result = 31 * _result + (tags != null ? tags.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "WrapBuilder {" +
				"inner=" + this.inner + ", " +
				"text=" + this.text + ", " +
				"tags=" + this.tags +
			'}';
		}
	}
}
