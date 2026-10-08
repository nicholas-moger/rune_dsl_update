package test.deeppathedge;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneChoiceType;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;
import test.deeppathedge.meta.OuterMeta;

import static java.util.Optional.ofNullable;

/**
 * The nested choice (the chaos C3Outer).
 * @version 0.0.0
 */
@RosettaDataType(value="Outer", builder=Outer.OuterBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Outer", model="test", builder=Outer.OuterBuilderImpl.class, version="0.0.0")
@RuneChoiceType
public interface Outer extends RosettaModelObject {

	OuterMeta metaData = new OuterMeta();

	/*********************** Getter Methods  ***********************/
	Wrap getWrap();
	Note getNote();

	/*********************** Build Methods  ***********************/
	Outer build();
	
	Outer.OuterBuilder toBuilder();
	
	static Outer.OuterBuilder builder() {
		return new Outer.OuterBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Outer> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Outer> getType() {
		return Outer.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("Wrap"), processor, Wrap.class, getWrap());
		processRosetta(path.newSubPath("Note"), processor, Note.class, getNote());
	}
	

	/*********************** Builder Interface  ***********************/
	interface OuterBuilder extends Outer, RosettaModelObjectBuilder {
		Wrap.WrapBuilder getOrCreateWrap();
		@Override
		Wrap.WrapBuilder getWrap();
		Note.NoteBuilder getOrCreateNote();
		@Override
		Note.NoteBuilder getNote();
		Outer.OuterBuilder setWrap(Wrap _Wrap);
		Outer.OuterBuilder setNote(Note _Note);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("Wrap"), processor, Wrap.WrapBuilder.class, getWrap());
			processRosetta(path.newSubPath("Note"), processor, Note.NoteBuilder.class, getNote());
		}
		

		Outer.OuterBuilder prune();
	}

	/*********************** Immutable Implementation of Outer  ***********************/
	class OuterImpl implements Outer {
		private final Wrap wrap;
		private final Note note;
		
		protected OuterImpl(Outer.OuterBuilder builder) {
			this.wrap = ofNullable(builder.getWrap()).map(f->f.build()).orElse(null);
			this.note = ofNullable(builder.getNote()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("Wrap")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Wrap")
		public Wrap getWrap() {
			return wrap;
		}
		
		@Override
		@RosettaAttribute("Note")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Note")
		public Note getNote() {
			return note;
		}
		
		@Override
		public Outer build() {
			return this;
		}
		
		@Override
		public Outer.OuterBuilder toBuilder() {
			Outer.OuterBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Outer.OuterBuilder builder) {
			ofNullable(getWrap()).ifPresent(builder::setWrap);
			ofNullable(getNote()).ifPresent(builder::setNote);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Outer _that = getType().cast(o);
		
			if (!Objects.equals(wrap, _that.getWrap())) return false;
			if (!Objects.equals(note, _that.getNote())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (wrap != null ? wrap.hashCode() : 0);
			_result = 31 * _result + (note != null ? note.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Outer {" +
				"Wrap=" + this.wrap + ", " +
				"Note=" + this.note +
			'}';
		}
	}

	/*********************** Builder Implementation of Outer  ***********************/
	class OuterBuilderImpl implements Outer.OuterBuilder {
	
		protected Wrap.WrapBuilder wrap;
		protected Note.NoteBuilder note;
		
		@Override
		@RosettaAttribute("Wrap")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Wrap")
		public Wrap.WrapBuilder getWrap() {
			return wrap;
		}
		
		@Override
		public Wrap.WrapBuilder getOrCreateWrap() {
			Wrap.WrapBuilder result;
			if (wrap!=null) {
				result = wrap;
			}
			else {
				result = wrap = Wrap.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("Note")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Note")
		public Note.NoteBuilder getNote() {
			return note;
		}
		
		@Override
		public Note.NoteBuilder getOrCreateNote() {
			Note.NoteBuilder result;
			if (note!=null) {
				result = note;
			}
			else {
				result = note = Note.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("Wrap")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("Wrap")
		@Override
		public Outer.OuterBuilder setWrap(Wrap _wrap) {
			this.wrap = _wrap == null ? null : _wrap.toBuilder();
			return this;
		}
		
		@RosettaAttribute("Note")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("Note")
		@Override
		public Outer.OuterBuilder setNote(Note _note) {
			this.note = _note == null ? null : _note.toBuilder();
			return this;
		}
		
		@Override
		public Outer build() {
			return new Outer.OuterImpl(this);
		}
		
		@Override
		public Outer.OuterBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Outer.OuterBuilder prune() {
			if (wrap!=null && !wrap.prune().hasData()) wrap = null;
			if (note!=null && !note.prune().hasData()) note = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getWrap()!=null && getWrap().hasData()) return true;
			if (getNote()!=null && getNote().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Outer.OuterBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Outer.OuterBuilder o = (Outer.OuterBuilder) other;
			
			merger.mergeRosetta(getWrap(), o.getWrap(), this::setWrap);
			merger.mergeRosetta(getNote(), o.getNote(), this::setNote);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Outer _that = getType().cast(o);
		
			if (!Objects.equals(wrap, _that.getWrap())) return false;
			if (!Objects.equals(note, _that.getNote())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (wrap != null ? wrap.hashCode() : 0);
			_result = 31 * _result + (note != null ? note.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OuterBuilder {" +
				"Wrap=" + this.wrap + ", " +
				"Note=" + this.note +
			'}';
		}
	}
}
