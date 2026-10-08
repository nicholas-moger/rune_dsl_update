package chaos.s29.a5uni;

import chaos.s29.a5uni.meta.C29OuterMeta;
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
 * The outer choice.
 * @version 1.0.0
 */
@RosettaDataType(value="C29Outer", builder=C29Outer.C29OuterBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C29Outer", model="chaos", builder=C29Outer.C29OuterBuilderImpl.class, version="1.0.0")
@RuneChoiceType
public interface C29Outer extends RosettaModelObject {

	C29OuterMeta metaData = new C29OuterMeta();

	/*********************** Getter Methods  ***********************/
	C29Note getC29Note();
	C29Wrap getC29Wrap();

	/*********************** Build Methods  ***********************/
	C29Outer build();
	
	C29Outer.C29OuterBuilder toBuilder();
	
	static C29Outer.C29OuterBuilder builder() {
		return new C29Outer.C29OuterBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C29Outer> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C29Outer> getType() {
		return C29Outer.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("C29Note"), processor, C29Note.class, getC29Note());
		processRosetta(path.newSubPath("C29Wrap"), processor, C29Wrap.class, getC29Wrap());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C29OuterBuilder extends C29Outer, RosettaModelObjectBuilder {
		C29Note.C29NoteBuilder getOrCreateC29Note();
		@Override
		C29Note.C29NoteBuilder getC29Note();
		C29Wrap.C29WrapBuilder getOrCreateC29Wrap();
		@Override
		C29Wrap.C29WrapBuilder getC29Wrap();
		C29Outer.C29OuterBuilder setC29Note(C29Note _C29Note);
		C29Outer.C29OuterBuilder setC29Wrap(C29Wrap _C29Wrap);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("C29Note"), processor, C29Note.C29NoteBuilder.class, getC29Note());
			processRosetta(path.newSubPath("C29Wrap"), processor, C29Wrap.C29WrapBuilder.class, getC29Wrap());
		}
		

		C29Outer.C29OuterBuilder prune();
	}

	/*********************** Immutable Implementation of C29Outer  ***********************/
	class C29OuterImpl implements C29Outer {
		private final C29Note c29Note;
		private final C29Wrap c29Wrap;
		
		protected C29OuterImpl(C29Outer.C29OuterBuilder builder) {
			this.c29Note = ofNullable(builder.getC29Note()).map(f->f.build()).orElse(null);
			this.c29Wrap = ofNullable(builder.getC29Wrap()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("C29Note")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C29Note")
		public C29Note getC29Note() {
			return c29Note;
		}
		
		@Override
		@RosettaAttribute("C29Wrap")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C29Wrap")
		public C29Wrap getC29Wrap() {
			return c29Wrap;
		}
		
		@Override
		public C29Outer build() {
			return this;
		}
		
		@Override
		public C29Outer.C29OuterBuilder toBuilder() {
			C29Outer.C29OuterBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C29Outer.C29OuterBuilder builder) {
			ofNullable(getC29Note()).ifPresent(builder::setC29Note);
			ofNullable(getC29Wrap()).ifPresent(builder::setC29Wrap);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C29Outer _that = getType().cast(o);
		
			if (!Objects.equals(c29Note, _that.getC29Note())) return false;
			if (!Objects.equals(c29Wrap, _that.getC29Wrap())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c29Note != null ? c29Note.hashCode() : 0);
			_result = 31 * _result + (c29Wrap != null ? c29Wrap.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C29Outer {" +
				"C29Note=" + this.c29Note + ", " +
				"C29Wrap=" + this.c29Wrap +
			'}';
		}
	}

	/*********************** Builder Implementation of C29Outer  ***********************/
	class C29OuterBuilderImpl implements C29Outer.C29OuterBuilder {
	
		protected C29Note.C29NoteBuilder c29Note;
		protected C29Wrap.C29WrapBuilder c29Wrap;
		
		@Override
		@RosettaAttribute("C29Note")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C29Note")
		public C29Note.C29NoteBuilder getC29Note() {
			return c29Note;
		}
		
		@Override
		public C29Note.C29NoteBuilder getOrCreateC29Note() {
			C29Note.C29NoteBuilder result;
			if (c29Note!=null) {
				result = c29Note;
			}
			else {
				result = c29Note = C29Note.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("C29Wrap")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C29Wrap")
		public C29Wrap.C29WrapBuilder getC29Wrap() {
			return c29Wrap;
		}
		
		@Override
		public C29Wrap.C29WrapBuilder getOrCreateC29Wrap() {
			C29Wrap.C29WrapBuilder result;
			if (c29Wrap!=null) {
				result = c29Wrap;
			}
			else {
				result = c29Wrap = C29Wrap.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("C29Note")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C29Note")
		@Override
		public C29Outer.C29OuterBuilder setC29Note(C29Note _c29Note) {
			this.c29Note = _c29Note == null ? null : _c29Note.toBuilder();
			return this;
		}
		
		@RosettaAttribute("C29Wrap")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C29Wrap")
		@Override
		public C29Outer.C29OuterBuilder setC29Wrap(C29Wrap _c29Wrap) {
			this.c29Wrap = _c29Wrap == null ? null : _c29Wrap.toBuilder();
			return this;
		}
		
		@Override
		public C29Outer build() {
			return new C29Outer.C29OuterImpl(this);
		}
		
		@Override
		public C29Outer.C29OuterBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C29Outer.C29OuterBuilder prune() {
			if (c29Note!=null && !c29Note.prune().hasData()) c29Note = null;
			if (c29Wrap!=null && !c29Wrap.prune().hasData()) c29Wrap = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getC29Note()!=null && getC29Note().hasData()) return true;
			if (getC29Wrap()!=null && getC29Wrap().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C29Outer.C29OuterBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C29Outer.C29OuterBuilder o = (C29Outer.C29OuterBuilder) other;
			
			merger.mergeRosetta(getC29Note(), o.getC29Note(), this::setC29Note);
			merger.mergeRosetta(getC29Wrap(), o.getC29Wrap(), this::setC29Wrap);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C29Outer _that = getType().cast(o);
		
			if (!Objects.equals(c29Note, _that.getC29Note())) return false;
			if (!Objects.equals(c29Wrap, _that.getC29Wrap())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c29Note != null ? c29Note.hashCode() : 0);
			_result = 31 * _result + (c29Wrap != null ? c29Wrap.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C29OuterBuilder {" +
				"C29Note=" + this.c29Note + ", " +
				"C29Wrap=" + this.c29Wrap +
			'}';
		}
	}
}
