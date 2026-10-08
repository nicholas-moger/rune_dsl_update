package test.deeppathedge;

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
import test.deeppathedge.meta.NoteMeta;

import static java.util.Optional.ofNullable;

/**
 * Choice option 1 - a text and tags (the chaos C3Note, widened by tags).
 * @version 0.0.0
 */
@RosettaDataType(value="Note", builder=Note.NoteBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Note", model="test", builder=Note.NoteBuilderImpl.class, version="0.0.0")
public interface Note extends RosettaModelObject {

	NoteMeta metaData = new NoteMeta();

	/*********************** Getter Methods  ***********************/
	String getText();
	List<String> getTags();

	/*********************** Build Methods  ***********************/
	Note build();
	
	Note.NoteBuilder toBuilder();
	
	static Note.NoteBuilder builder() {
		return new Note.NoteBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Note> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Note> getType() {
		return Note.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("text"), String.class, getText(), this);
		processor.processBasic(path.newSubPath("tags"), String.class, getTags(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface NoteBuilder extends Note, RosettaModelObjectBuilder {
		Note.NoteBuilder setText(String text);
		Note.NoteBuilder addTags(String tags);
		Note.NoteBuilder addTags(String tags, int idx);
		Note.NoteBuilder addTags(List<String> tags);
		Note.NoteBuilder setTags(List<String> tags);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("text"), String.class, getText(), this);
			processor.processBasic(path.newSubPath("tags"), String.class, getTags(), this);
		}
		

		Note.NoteBuilder prune();
	}

	/*********************** Immutable Implementation of Note  ***********************/
	class NoteImpl implements Note {
		private final String text;
		private final List<String> tags;
		
		protected NoteImpl(Note.NoteBuilder builder) {
			this.text = builder.getText();
			this.tags = ofNullable(builder.getTags()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("text")
		@Accessor(AccessorType.GETTER)
		@Required
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
		public Note build() {
			return this;
		}
		
		@Override
		public Note.NoteBuilder toBuilder() {
			Note.NoteBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Note.NoteBuilder builder) {
			ofNullable(getText()).ifPresent(builder::setText);
			ofNullable(getTags()).ifPresent(builder::setTags);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Note _that = getType().cast(o);
		
			if (!Objects.equals(text, _that.getText())) return false;
			if (!ListEquals.listEquals(tags, _that.getTags())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (text != null ? text.hashCode() : 0);
			_result = 31 * _result + (tags != null ? tags.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Note {" +
				"text=" + this.text + ", " +
				"tags=" + this.tags +
			'}';
		}
	}

	/*********************** Builder Implementation of Note  ***********************/
	class NoteBuilderImpl implements Note.NoteBuilder {
	
		protected String text;
		protected List<String> tags = new ArrayList<>();
		
		@Override
		@RosettaAttribute("text")
		@Accessor(AccessorType.GETTER)
		@Required
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
		
		@RosettaAttribute("text")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("text")
		@Override
		public Note.NoteBuilder setText(String _text) {
			this.text = _text == null ? null : _text;
			return this;
		}
		
		@RosettaAttribute("tags")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("tags")
		@Override
		public Note.NoteBuilder addTags(String _tags) {
			if (_tags != null) {
				this.tags.add(_tags);
			}
			return this;
		}
		
		@Override
		public Note.NoteBuilder addTags(String _tags, int idx) {
			getIndex(this.tags, idx, () -> _tags);
			return this;
		}
		
		@Override
		public Note.NoteBuilder addTags(List<String> tagss) {
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
		public Note.NoteBuilder setTags(List<String> tagss) {
			if (tagss == null) {
				this.tags = new ArrayList<>();
			} else {
				this.tags = tagss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Note build() {
			return new Note.NoteImpl(this);
		}
		
		@Override
		public Note.NoteBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Note.NoteBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getText()!=null) return true;
			if (getTags()!=null && !getTags().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Note.NoteBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Note.NoteBuilder o = (Note.NoteBuilder) other;
			
			
			merger.mergeBasic(getText(), o.getText(), this::setText);
			merger.mergeBasic(getTags(), o.getTags(), (Consumer<String>) this::addTags);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Note _that = getType().cast(o);
		
			if (!Objects.equals(text, _that.getText())) return false;
			if (!ListEquals.listEquals(tags, _that.getTags())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (text != null ? text.hashCode() : 0);
			_result = 31 * _result + (tags != null ? tags.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "NoteBuilder {" +
				"text=" + this.text + ", " +
				"tags=" + this.tags +
			'}';
		}
	}
}
