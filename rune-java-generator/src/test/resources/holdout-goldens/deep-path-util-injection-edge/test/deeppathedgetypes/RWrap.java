package test.deeppathedgetypes;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
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
import test.deeppathedgetypes.meta.RWrapMeta;

import static java.util.Optional.ofNullable;

/**
 * Remote option 2.
 * @version 0.0.0
 */
@RosettaDataType(value="RWrap", builder=RWrap.RWrapBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RWrap", model="test", builder=RWrap.RWrapBuilderImpl.class, version="0.0.0")
public interface RWrap extends RosettaModelObject {

	RWrapMeta metaData = new RWrapMeta();

	/*********************** Getter Methods  ***********************/
	String getText();

	/*********************** Build Methods  ***********************/
	RWrap build();
	
	RWrap.RWrapBuilder toBuilder();
	
	static RWrap.RWrapBuilder builder() {
		return new RWrap.RWrapBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RWrap> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RWrap> getType() {
		return RWrap.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("text"), String.class, getText(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RWrapBuilder extends RWrap, RosettaModelObjectBuilder {
		RWrap.RWrapBuilder setText(String text);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("text"), String.class, getText(), this);
		}
		

		RWrap.RWrapBuilder prune();
	}

	/*********************** Immutable Implementation of RWrap  ***********************/
	class RWrapImpl implements RWrap {
		private final String text;
		
		protected RWrapImpl(RWrap.RWrapBuilder builder) {
			this.text = builder.getText();
		}
		
		@Override
		@RosettaAttribute("text")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("text")
		public String getText() {
			return text;
		}
		
		@Override
		public RWrap build() {
			return this;
		}
		
		@Override
		public RWrap.RWrapBuilder toBuilder() {
			RWrap.RWrapBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RWrap.RWrapBuilder builder) {
			ofNullable(getText()).ifPresent(builder::setText);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RWrap _that = getType().cast(o);
		
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
			return "RWrap {" +
				"text=" + this.text +
			'}';
		}
	}

	/*********************** Builder Implementation of RWrap  ***********************/
	class RWrapBuilderImpl implements RWrap.RWrapBuilder {
	
		protected String text;
		
		@Override
		@RosettaAttribute("text")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("text")
		public String getText() {
			return text;
		}
		
		@RosettaAttribute("text")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("text")
		@Override
		public RWrap.RWrapBuilder setText(String _text) {
			this.text = _text == null ? null : _text;
			return this;
		}
		
		@Override
		public RWrap build() {
			return new RWrap.RWrapImpl(this);
		}
		
		@Override
		public RWrap.RWrapBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RWrap.RWrapBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getText()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RWrap.RWrapBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RWrap.RWrapBuilder o = (RWrap.RWrapBuilder) other;
			
			
			merger.mergeBasic(getText(), o.getText(), this::setText);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RWrap _that = getType().cast(o);
		
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
			return "RWrapBuilder {" +
				"text=" + this.text +
			'}';
		}
	}
}
