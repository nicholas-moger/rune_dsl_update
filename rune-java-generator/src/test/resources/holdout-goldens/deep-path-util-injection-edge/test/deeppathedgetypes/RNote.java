package test.deeppathedgetypes;

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
import test.deeppathedgetypes.meta.RNoteMeta;

import static java.util.Optional.ofNullable;

/**
 * Remote option 1.
 * @version 0.0.0
 */
@RosettaDataType(value="RNote", builder=RNote.RNoteBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RNote", model="test", builder=RNote.RNoteBuilderImpl.class, version="0.0.0")
public interface RNote extends RosettaModelObject {

	RNoteMeta metaData = new RNoteMeta();

	/*********************** Getter Methods  ***********************/
	String getText();

	/*********************** Build Methods  ***********************/
	RNote build();
	
	RNote.RNoteBuilder toBuilder();
	
	static RNote.RNoteBuilder builder() {
		return new RNote.RNoteBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RNote> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RNote> getType() {
		return RNote.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("text"), String.class, getText(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RNoteBuilder extends RNote, RosettaModelObjectBuilder {
		RNote.RNoteBuilder setText(String text);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("text"), String.class, getText(), this);
		}
		

		RNote.RNoteBuilder prune();
	}

	/*********************** Immutable Implementation of RNote  ***********************/
	class RNoteImpl implements RNote {
		private final String text;
		
		protected RNoteImpl(RNote.RNoteBuilder builder) {
			this.text = builder.getText();
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
		public RNote build() {
			return this;
		}
		
		@Override
		public RNote.RNoteBuilder toBuilder() {
			RNote.RNoteBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RNote.RNoteBuilder builder) {
			ofNullable(getText()).ifPresent(builder::setText);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RNote _that = getType().cast(o);
		
			if (!Objects.equals(text, _that.getText())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (text != null ? text.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RNote {" +
				"text=" + this.text +
			'}';
		}
	}

	/*********************** Builder Implementation of RNote  ***********************/
	class RNoteBuilderImpl implements RNote.RNoteBuilder {
	
		protected String text;
		
		@Override
		@RosettaAttribute("text")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("text")
		public String getText() {
			return text;
		}
		
		@RosettaAttribute("text")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("text")
		@Override
		public RNote.RNoteBuilder setText(String _text) {
			this.text = _text == null ? null : _text;
			return this;
		}
		
		@Override
		public RNote build() {
			return new RNote.RNoteImpl(this);
		}
		
		@Override
		public RNote.RNoteBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RNote.RNoteBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getText()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RNote.RNoteBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RNote.RNoteBuilder o = (RNote.RNoteBuilder) other;
			
			
			merger.mergeBasic(getText(), o.getText(), this::setText);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RNote _that = getType().cast(o);
		
			if (!Objects.equals(text, _that.getText())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (text != null ? text.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RNoteBuilder {" +
				"text=" + this.text +
			'}';
		}
	}
}
