package chaos.s03.a4snap;

import chaos.s03.a4snap.meta.C3NoteMeta;
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
 * Choice option AND helper - relocated by the import axis.
 * @version 1.0.0-SNAPSHOT
 */
@RosettaDataType(value="C3Note", builder=C3Note.C3NoteBuilderImpl.class, version="1.0.0-SNAPSHOT")
@RuneDataType(value="C3Note", model="chaos", builder=C3Note.C3NoteBuilderImpl.class, version="1.0.0-SNAPSHOT")
public interface C3Note extends RosettaModelObject {

	C3NoteMeta metaData = new C3NoteMeta();

	/*********************** Getter Methods  ***********************/
	String getText();

	/*********************** Build Methods  ***********************/
	C3Note build();
	
	C3Note.C3NoteBuilder toBuilder();
	
	static C3Note.C3NoteBuilder builder() {
		return new C3Note.C3NoteBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C3Note> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C3Note> getType() {
		return C3Note.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("text"), String.class, getText(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C3NoteBuilder extends C3Note, RosettaModelObjectBuilder {
		C3Note.C3NoteBuilder setText(String text);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("text"), String.class, getText(), this);
		}
		

		C3Note.C3NoteBuilder prune();
	}

	/*********************** Immutable Implementation of C3Note  ***********************/
	class C3NoteImpl implements C3Note {
		private final String text;
		
		protected C3NoteImpl(C3Note.C3NoteBuilder builder) {
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
		public C3Note build() {
			return this;
		}
		
		@Override
		public C3Note.C3NoteBuilder toBuilder() {
			C3Note.C3NoteBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C3Note.C3NoteBuilder builder) {
			ofNullable(getText()).ifPresent(builder::setText);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C3Note _that = getType().cast(o);
		
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
			return "C3Note {" +
				"text=" + this.text +
			'}';
		}
	}

	/*********************** Builder Implementation of C3Note  ***********************/
	class C3NoteBuilderImpl implements C3Note.C3NoteBuilder {
	
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
		public C3Note.C3NoteBuilder setText(String _text) {
			this.text = _text == null ? null : _text;
			return this;
		}
		
		@Override
		public C3Note build() {
			return new C3Note.C3NoteImpl(this);
		}
		
		@Override
		public C3Note.C3NoteBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C3Note.C3NoteBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getText()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C3Note.C3NoteBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C3Note.C3NoteBuilder o = (C3Note.C3NoteBuilder) other;
			
			
			merger.mergeBasic(getText(), o.getText(), this::setText);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C3Note _that = getType().cast(o);
		
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
			return "C3NoteBuilder {" +
				"text=" + this.text +
			'}';
		}
	}
}
