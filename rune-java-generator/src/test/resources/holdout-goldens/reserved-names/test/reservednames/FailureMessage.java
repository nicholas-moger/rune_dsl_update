package test.reservednames;

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
import test.reservednames.meta.FailureMessageMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="FailureMessage", builder=FailureMessage.FailureMessageBuilderImpl.class, version="0.0.0")
@RuneDataType(value="FailureMessage", model="test", builder=FailureMessage.FailureMessageBuilderImpl.class, version="0.0.0")
public interface FailureMessage extends RosettaModelObject {

	FailureMessageMeta metaData = new FailureMessageMeta();

	/*********************** Getter Methods  ***********************/
	String getText();

	/*********************** Build Methods  ***********************/
	FailureMessage build();
	
	FailureMessage.FailureMessageBuilder toBuilder();
	
	static FailureMessage.FailureMessageBuilder builder() {
		return new FailureMessage.FailureMessageBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends FailureMessage> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends FailureMessage> getType() {
		return FailureMessage.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("text"), String.class, getText(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface FailureMessageBuilder extends FailureMessage, RosettaModelObjectBuilder {
		FailureMessage.FailureMessageBuilder setText(String text);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("text"), String.class, getText(), this);
		}
		

		FailureMessage.FailureMessageBuilder prune();
	}

	/*********************** Immutable Implementation of FailureMessage  ***********************/
	class FailureMessageImpl implements FailureMessage {
		private final String text;
		
		protected FailureMessageImpl(FailureMessage.FailureMessageBuilder builder) {
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
		public FailureMessage build() {
			return this;
		}
		
		@Override
		public FailureMessage.FailureMessageBuilder toBuilder() {
			FailureMessage.FailureMessageBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(FailureMessage.FailureMessageBuilder builder) {
			ofNullable(getText()).ifPresent(builder::setText);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FailureMessage _that = getType().cast(o);
		
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
			return "FailureMessage {" +
				"text=" + this.text +
			'}';
		}
	}

	/*********************** Builder Implementation of FailureMessage  ***********************/
	class FailureMessageBuilderImpl implements FailureMessage.FailureMessageBuilder {
	
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
		public FailureMessage.FailureMessageBuilder setText(String _text) {
			this.text = _text == null ? null : _text;
			return this;
		}
		
		@Override
		public FailureMessage build() {
			return new FailureMessage.FailureMessageImpl(this);
		}
		
		@Override
		public FailureMessage.FailureMessageBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FailureMessage.FailureMessageBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getText()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FailureMessage.FailureMessageBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			FailureMessage.FailureMessageBuilder o = (FailureMessage.FailureMessageBuilder) other;
			
			
			merger.mergeBasic(getText(), o.getText(), this::setText);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FailureMessage _that = getType().cast(o);
		
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
			return "FailureMessageBuilder {" +
				"text=" + this.text +
			'}';
		}
	}
}
