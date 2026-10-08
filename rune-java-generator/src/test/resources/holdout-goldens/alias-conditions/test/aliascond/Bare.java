package test.aliascond;

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
import com.rosetta.model.metafields.FieldWithMetaString;
import java.math.BigDecimal;
import java.util.Objects;
import test.aliascond.meta.BareMeta;

import static java.util.Optional.ofNullable;

/**
 * A type with NO alias-conditioned attribute - the simple wing only.
 * @version 0.0.0
 */
@RosettaDataType(value="Bare", builder=Bare.BareBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Bare", model="test", builder=Bare.BareBuilderImpl.class, version="0.0.0")
public interface Bare extends RosettaModelObject {

	BareMeta metaData = new BareMeta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getQty();
	FieldWithMetaString getText();

	/*********************** Build Methods  ***********************/
	Bare build();
	
	Bare.BareBuilder toBuilder();
	
	static Bare.BareBuilder builder() {
		return new Bare.BareBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Bare> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Bare> getType() {
		return Bare.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("qty"), BigDecimal.class, getQty(), this);
		processRosetta(path.newSubPath("text"), processor, FieldWithMetaString.class, getText());
	}
	

	/*********************** Builder Interface  ***********************/
	interface BareBuilder extends Bare, RosettaModelObjectBuilder {
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateText();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getText();
		Bare.BareBuilder setQty(BigDecimal qty);
		Bare.BareBuilder setText(FieldWithMetaString text);
		Bare.BareBuilder setTextValue(String text);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("qty"), BigDecimal.class, getQty(), this);
			processRosetta(path.newSubPath("text"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getText());
		}
		

		Bare.BareBuilder prune();
	}

	/*********************** Immutable Implementation of Bare  ***********************/
	class BareImpl implements Bare {
		private final BigDecimal qty;
		private final FieldWithMetaString text;
		
		protected BareImpl(Bare.BareBuilder builder) {
			this.qty = builder.getQty();
			this.text = ofNullable(builder.getText()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("qty")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("qty")
		public BigDecimal getQty() {
			return qty;
		}
		
		@Override
		@RosettaAttribute("text")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("text")
		public FieldWithMetaString getText() {
			return text;
		}
		
		@Override
		public Bare build() {
			return this;
		}
		
		@Override
		public Bare.BareBuilder toBuilder() {
			Bare.BareBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Bare.BareBuilder builder) {
			ofNullable(getQty()).ifPresent(builder::setQty);
			ofNullable(getText()).ifPresent(builder::setText);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Bare _that = getType().cast(o);
		
			if (!Objects.equals(qty, _that.getQty())) return false;
			if (!Objects.equals(text, _that.getText())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (qty != null ? qty.hashCode() : 0);
			_result = 31 * _result + (text != null ? text.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Bare {" +
				"qty=" + this.qty + ", " +
				"text=" + this.text +
			'}';
		}
	}

	/*********************** Builder Implementation of Bare  ***********************/
	class BareBuilderImpl implements Bare.BareBuilder {
	
		protected BigDecimal qty;
		protected FieldWithMetaString.FieldWithMetaStringBuilder text;
		
		@Override
		@RosettaAttribute("qty")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("qty")
		public BigDecimal getQty() {
			return qty;
		}
		
		@Override
		@RosettaAttribute("text")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("text")
		public FieldWithMetaString.FieldWithMetaStringBuilder getText() {
			return text;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateText() {
			FieldWithMetaString.FieldWithMetaStringBuilder result;
			if (text!=null) {
				result = text;
			}
			else {
				result = text = FieldWithMetaString.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("qty")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("qty")
		@Override
		public Bare.BareBuilder setQty(BigDecimal _qty) {
			this.qty = _qty == null ? null : _qty;
			return this;
		}
		
		@RosettaAttribute("text")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("text")
		@Override
		public Bare.BareBuilder setText(FieldWithMetaString _text) {
			this.text = _text == null ? null : _text.toBuilder();
			return this;
		}
		
		@Override
		public Bare.BareBuilder setTextValue(String _text) {
			this.getOrCreateText().setValue(_text);
			return this;
		}
		
		@Override
		public Bare build() {
			return new Bare.BareImpl(this);
		}
		
		@Override
		public Bare.BareBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Bare.BareBuilder prune() {
			if (text!=null && !text.prune().hasData()) text = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getQty()!=null) return true;
			if (getText()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Bare.BareBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Bare.BareBuilder o = (Bare.BareBuilder) other;
			
			merger.mergeRosetta(getText(), o.getText(), this::setText);
			
			merger.mergeBasic(getQty(), o.getQty(), this::setQty);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Bare _that = getType().cast(o);
		
			if (!Objects.equals(qty, _that.getQty())) return false;
			if (!Objects.equals(text, _that.getText())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (qty != null ? qty.hashCode() : 0);
			_result = 31 * _result + (text != null ? text.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BareBuilder {" +
				"qty=" + this.qty + ", " +
				"text=" + this.text +
			'}';
		}
	}
}
