package chaos.s03.a1o3;

import chaos.s03.a1o3.meta.C3WrapMeta;
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
 * Wraps the inner choice; shares &#39;text&#39; with C3Note.
 * @version 1.0.0
 */
@RosettaDataType(value="C3Wrap", builder=C3Wrap.C3WrapBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C3Wrap", model="chaos", builder=C3Wrap.C3WrapBuilderImpl.class, version="1.0.0")
public interface C3Wrap extends RosettaModelObject {

	C3WrapMeta metaData = new C3WrapMeta();

	/*********************** Getter Methods  ***********************/
	C3Inner getInner();
	String getText();

	/*********************** Build Methods  ***********************/
	C3Wrap build();
	
	C3Wrap.C3WrapBuilder toBuilder();
	
	static C3Wrap.C3WrapBuilder builder() {
		return new C3Wrap.C3WrapBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C3Wrap> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C3Wrap> getType() {
		return C3Wrap.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("inner"), processor, C3Inner.class, getInner());
		processor.processBasic(path.newSubPath("text"), String.class, getText(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C3WrapBuilder extends C3Wrap, RosettaModelObjectBuilder {
		C3Inner.C3InnerBuilder getOrCreateInner();
		@Override
		C3Inner.C3InnerBuilder getInner();
		C3Wrap.C3WrapBuilder setInner(C3Inner inner);
		C3Wrap.C3WrapBuilder setText(String text);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("inner"), processor, C3Inner.C3InnerBuilder.class, getInner());
			processor.processBasic(path.newSubPath("text"), String.class, getText(), this);
		}
		

		C3Wrap.C3WrapBuilder prune();
	}

	/*********************** Immutable Implementation of C3Wrap  ***********************/
	class C3WrapImpl implements C3Wrap {
		private final C3Inner inner;
		private final String text;
		
		protected C3WrapImpl(C3Wrap.C3WrapBuilder builder) {
			this.inner = ofNullable(builder.getInner()).map(f->f.build()).orElse(null);
			this.text = builder.getText();
		}
		
		@Override
		@RosettaAttribute("inner")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("inner")
		public C3Inner getInner() {
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
		public C3Wrap build() {
			return this;
		}
		
		@Override
		public C3Wrap.C3WrapBuilder toBuilder() {
			C3Wrap.C3WrapBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C3Wrap.C3WrapBuilder builder) {
			ofNullable(getInner()).ifPresent(builder::setInner);
			ofNullable(getText()).ifPresent(builder::setText);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C3Wrap _that = getType().cast(o);
		
			if (!Objects.equals(inner, _that.getInner())) return false;
			if (!Objects.equals(text, _that.getText())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (inner != null ? inner.hashCode() : 0);
			_result = 31 * _result + (text != null ? text.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C3Wrap {" +
				"inner=" + this.inner + ", " +
				"text=" + this.text +
			'}';
		}
	}

	/*********************** Builder Implementation of C3Wrap  ***********************/
	class C3WrapBuilderImpl implements C3Wrap.C3WrapBuilder {
	
		protected C3Inner.C3InnerBuilder inner;
		protected String text;
		
		@Override
		@RosettaAttribute("inner")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("inner")
		public C3Inner.C3InnerBuilder getInner() {
			return inner;
		}
		
		@Override
		public C3Inner.C3InnerBuilder getOrCreateInner() {
			C3Inner.C3InnerBuilder result;
			if (inner!=null) {
				result = inner;
			}
			else {
				result = inner = C3Inner.builder();
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
		
		@RosettaAttribute("inner")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("inner")
		@Override
		public C3Wrap.C3WrapBuilder setInner(C3Inner _inner) {
			this.inner = _inner == null ? null : _inner.toBuilder();
			return this;
		}
		
		@RosettaAttribute("text")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("text")
		@Override
		public C3Wrap.C3WrapBuilder setText(String _text) {
			this.text = _text == null ? null : _text;
			return this;
		}
		
		@Override
		public C3Wrap build() {
			return new C3Wrap.C3WrapImpl(this);
		}
		
		@Override
		public C3Wrap.C3WrapBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C3Wrap.C3WrapBuilder prune() {
			if (inner!=null && !inner.prune().hasData()) inner = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getInner()!=null && getInner().hasData()) return true;
			if (getText()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C3Wrap.C3WrapBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C3Wrap.C3WrapBuilder o = (C3Wrap.C3WrapBuilder) other;
			
			merger.mergeRosetta(getInner(), o.getInner(), this::setInner);
			
			merger.mergeBasic(getText(), o.getText(), this::setText);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C3Wrap _that = getType().cast(o);
		
			if (!Objects.equals(inner, _that.getInner())) return false;
			if (!Objects.equals(text, _that.getText())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (inner != null ? inner.hashCode() : 0);
			_result = 31 * _result + (text != null ? text.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C3WrapBuilder {" +
				"inner=" + this.inner + ", " +
				"text=" + this.text +
			'}';
		}
	}
}
