package holdout.closureparamduplicate;

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
import holdout.closureparamduplicate.meta.ItemMeta;
import java.math.BigDecimal;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * The M12 carrier&#39;s element (v3.2 seat 12, D52 H1): a number-valued item reduced over.
 * @version 0.0.0
 */
@RosettaDataType(value="Item", builder=Item.ItemBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Item", model="holdout", builder=Item.ItemBuilderImpl.class, version="0.0.0")
public interface Item extends RosettaModelObject {

	ItemMeta metaData = new ItemMeta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getV();

	/*********************** Build Methods  ***********************/
	Item build();
	
	Item.ItemBuilder toBuilder();
	
	static Item.ItemBuilder builder() {
		return new Item.ItemBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Item> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Item> getType() {
		return Item.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("v"), BigDecimal.class, getV(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface ItemBuilder extends Item, RosettaModelObjectBuilder {
		Item.ItemBuilder setV(BigDecimal v);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("v"), BigDecimal.class, getV(), this);
		}
		

		Item.ItemBuilder prune();
	}

	/*********************** Immutable Implementation of Item  ***********************/
	class ItemImpl implements Item {
		private final BigDecimal v;
		
		protected ItemImpl(Item.ItemBuilder builder) {
			this.v = builder.getV();
		}
		
		@Override
		@RosettaAttribute("v")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("v")
		public BigDecimal getV() {
			return v;
		}
		
		@Override
		public Item build() {
			return this;
		}
		
		@Override
		public Item.ItemBuilder toBuilder() {
			Item.ItemBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Item.ItemBuilder builder) {
			ofNullable(getV()).ifPresent(builder::setV);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Item _that = getType().cast(o);
		
			if (!Objects.equals(v, _that.getV())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (v != null ? v.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Item {" +
				"v=" + this.v +
			'}';
		}
	}

	/*********************** Builder Implementation of Item  ***********************/
	class ItemBuilderImpl implements Item.ItemBuilder {
	
		protected BigDecimal v;
		
		@Override
		@RosettaAttribute("v")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("v")
		public BigDecimal getV() {
			return v;
		}
		
		@RosettaAttribute("v")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("v")
		@Override
		public Item.ItemBuilder setV(BigDecimal _v) {
			this.v = _v == null ? null : _v;
			return this;
		}
		
		@Override
		public Item build() {
			return new Item.ItemImpl(this);
		}
		
		@Override
		public Item.ItemBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Item.ItemBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getV()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Item.ItemBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Item.ItemBuilder o = (Item.ItemBuilder) other;
			
			
			merger.mergeBasic(getV(), o.getV(), this::setV);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Item _that = getType().cast(o);
		
			if (!Objects.equals(v, _that.getV())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (v != null ? v.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ItemBuilder {" +
				"v=" + this.v +
			'}';
		}
	}
}
