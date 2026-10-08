package chaos.s20.a3half.p1;

import chaos.s20.a3half.p1.meta.C20LeafMeta;
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
import java.math.BigDecimal;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Depth-4 leaf - relocated by the import axis.
 * @version 1.0.0
 */
@RosettaDataType(value="C20Leaf", builder=C20Leaf.C20LeafBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C20Leaf", model="chaos", builder=C20Leaf.C20LeafBuilderImpl.class, version="1.0.0")
public interface C20Leaf extends RosettaModelObject {

	C20LeafMeta metaData = new C20LeafMeta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getV();

	/*********************** Build Methods  ***********************/
	C20Leaf build();
	
	C20Leaf.C20LeafBuilder toBuilder();
	
	static C20Leaf.C20LeafBuilder builder() {
		return new C20Leaf.C20LeafBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C20Leaf> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C20Leaf> getType() {
		return C20Leaf.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("v"), BigDecimal.class, getV(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C20LeafBuilder extends C20Leaf, RosettaModelObjectBuilder {
		C20Leaf.C20LeafBuilder setV(BigDecimal v);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("v"), BigDecimal.class, getV(), this);
		}
		

		C20Leaf.C20LeafBuilder prune();
	}

	/*********************** Immutable Implementation of C20Leaf  ***********************/
	class C20LeafImpl implements C20Leaf {
		private final BigDecimal v;
		
		protected C20LeafImpl(C20Leaf.C20LeafBuilder builder) {
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
		public C20Leaf build() {
			return this;
		}
		
		@Override
		public C20Leaf.C20LeafBuilder toBuilder() {
			C20Leaf.C20LeafBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C20Leaf.C20LeafBuilder builder) {
			ofNullable(getV()).ifPresent(builder::setV);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C20Leaf _that = getType().cast(o);
		
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
			return "C20Leaf {" +
				"v=" + this.v +
			'}';
		}
	}

	/*********************** Builder Implementation of C20Leaf  ***********************/
	class C20LeafBuilderImpl implements C20Leaf.C20LeafBuilder {
	
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
		public C20Leaf.C20LeafBuilder setV(BigDecimal _v) {
			this.v = _v == null ? null : _v;
			return this;
		}
		
		@Override
		public C20Leaf build() {
			return new C20Leaf.C20LeafImpl(this);
		}
		
		@Override
		public C20Leaf.C20LeafBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C20Leaf.C20LeafBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getV()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C20Leaf.C20LeafBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C20Leaf.C20LeafBuilder o = (C20Leaf.C20LeafBuilder) other;
			
			
			merger.mergeBasic(getV(), o.getV(), this::setV);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C20Leaf _that = getType().cast(o);
		
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
			return "C20LeafBuilder {" +
				"v=" + this.v +
			'}';
		}
	}
}
