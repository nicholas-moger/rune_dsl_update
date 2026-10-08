package chaos.s03.a1o3;

import chaos.s03.a1o3.meta.C3OuterMeta;
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

import static java.util.Optional.ofNullable;

/**
 * Nested choice - an option that itself wraps a choice.
 * @version 1.0.0
 */
@RosettaDataType(value="C3Outer", builder=C3Outer.C3OuterBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C3Outer", model="chaos", builder=C3Outer.C3OuterBuilderImpl.class, version="1.0.0")
@RuneChoiceType
public interface C3Outer extends RosettaModelObject {

	C3OuterMeta metaData = new C3OuterMeta();

	/*********************** Getter Methods  ***********************/
	C3Wrap getC3Wrap();
	C3Note getC3Note();

	/*********************** Build Methods  ***********************/
	C3Outer build();
	
	C3Outer.C3OuterBuilder toBuilder();
	
	static C3Outer.C3OuterBuilder builder() {
		return new C3Outer.C3OuterBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C3Outer> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C3Outer> getType() {
		return C3Outer.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("C3Wrap"), processor, C3Wrap.class, getC3Wrap());
		processRosetta(path.newSubPath("C3Note"), processor, C3Note.class, getC3Note());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C3OuterBuilder extends C3Outer, RosettaModelObjectBuilder {
		C3Wrap.C3WrapBuilder getOrCreateC3Wrap();
		@Override
		C3Wrap.C3WrapBuilder getC3Wrap();
		C3Note.C3NoteBuilder getOrCreateC3Note();
		@Override
		C3Note.C3NoteBuilder getC3Note();
		C3Outer.C3OuterBuilder setC3Wrap(C3Wrap _C3Wrap);
		C3Outer.C3OuterBuilder setC3Note(C3Note _C3Note);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("C3Wrap"), processor, C3Wrap.C3WrapBuilder.class, getC3Wrap());
			processRosetta(path.newSubPath("C3Note"), processor, C3Note.C3NoteBuilder.class, getC3Note());
		}
		

		C3Outer.C3OuterBuilder prune();
	}

	/*********************** Immutable Implementation of C3Outer  ***********************/
	class C3OuterImpl implements C3Outer {
		private final C3Wrap c3Wrap;
		private final C3Note c3Note;
		
		protected C3OuterImpl(C3Outer.C3OuterBuilder builder) {
			this.c3Wrap = ofNullable(builder.getC3Wrap()).map(f->f.build()).orElse(null);
			this.c3Note = ofNullable(builder.getC3Note()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("C3Wrap")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C3Wrap")
		public C3Wrap getC3Wrap() {
			return c3Wrap;
		}
		
		@Override
		@RosettaAttribute("C3Note")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C3Note")
		public C3Note getC3Note() {
			return c3Note;
		}
		
		@Override
		public C3Outer build() {
			return this;
		}
		
		@Override
		public C3Outer.C3OuterBuilder toBuilder() {
			C3Outer.C3OuterBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C3Outer.C3OuterBuilder builder) {
			ofNullable(getC3Wrap()).ifPresent(builder::setC3Wrap);
			ofNullable(getC3Note()).ifPresent(builder::setC3Note);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C3Outer _that = getType().cast(o);
		
			if (!Objects.equals(c3Wrap, _that.getC3Wrap())) return false;
			if (!Objects.equals(c3Note, _that.getC3Note())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c3Wrap != null ? c3Wrap.hashCode() : 0);
			_result = 31 * _result + (c3Note != null ? c3Note.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C3Outer {" +
				"C3Wrap=" + this.c3Wrap + ", " +
				"C3Note=" + this.c3Note +
			'}';
		}
	}

	/*********************** Builder Implementation of C3Outer  ***********************/
	class C3OuterBuilderImpl implements C3Outer.C3OuterBuilder {
	
		protected C3Wrap.C3WrapBuilder c3Wrap;
		protected C3Note.C3NoteBuilder c3Note;
		
		@Override
		@RosettaAttribute("C3Wrap")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C3Wrap")
		public C3Wrap.C3WrapBuilder getC3Wrap() {
			return c3Wrap;
		}
		
		@Override
		public C3Wrap.C3WrapBuilder getOrCreateC3Wrap() {
			C3Wrap.C3WrapBuilder result;
			if (c3Wrap!=null) {
				result = c3Wrap;
			}
			else {
				result = c3Wrap = C3Wrap.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("C3Note")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C3Note")
		public C3Note.C3NoteBuilder getC3Note() {
			return c3Note;
		}
		
		@Override
		public C3Note.C3NoteBuilder getOrCreateC3Note() {
			C3Note.C3NoteBuilder result;
			if (c3Note!=null) {
				result = c3Note;
			}
			else {
				result = c3Note = C3Note.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("C3Wrap")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C3Wrap")
		@Override
		public C3Outer.C3OuterBuilder setC3Wrap(C3Wrap _c3Wrap) {
			this.c3Wrap = _c3Wrap == null ? null : _c3Wrap.toBuilder();
			return this;
		}
		
		@RosettaAttribute("C3Note")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C3Note")
		@Override
		public C3Outer.C3OuterBuilder setC3Note(C3Note _c3Note) {
			this.c3Note = _c3Note == null ? null : _c3Note.toBuilder();
			return this;
		}
		
		@Override
		public C3Outer build() {
			return new C3Outer.C3OuterImpl(this);
		}
		
		@Override
		public C3Outer.C3OuterBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C3Outer.C3OuterBuilder prune() {
			if (c3Wrap!=null && !c3Wrap.prune().hasData()) c3Wrap = null;
			if (c3Note!=null && !c3Note.prune().hasData()) c3Note = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getC3Wrap()!=null && getC3Wrap().hasData()) return true;
			if (getC3Note()!=null && getC3Note().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C3Outer.C3OuterBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C3Outer.C3OuterBuilder o = (C3Outer.C3OuterBuilder) other;
			
			merger.mergeRosetta(getC3Wrap(), o.getC3Wrap(), this::setC3Wrap);
			merger.mergeRosetta(getC3Note(), o.getC3Note(), this::setC3Note);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C3Outer _that = getType().cast(o);
		
			if (!Objects.equals(c3Wrap, _that.getC3Wrap())) return false;
			if (!Objects.equals(c3Note, _that.getC3Note())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c3Wrap != null ? c3Wrap.hashCode() : 0);
			_result = 31 * _result + (c3Note != null ? c3Note.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C3OuterBuilder {" +
				"C3Wrap=" + this.c3Wrap + ", " +
				"C3Note=" + this.c3Note +
			'}';
		}
	}
}
