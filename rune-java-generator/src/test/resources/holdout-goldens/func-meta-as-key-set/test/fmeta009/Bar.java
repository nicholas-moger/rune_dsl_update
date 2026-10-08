package test.fmeta009;

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
import test.fmeta009.meta.BarMeta;
import test.fmeta009.metafields.ReferenceWithMetaFoo;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Bar", builder=Bar.BarBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Bar", model="test", builder=Bar.BarBuilderImpl.class, version="0.0.0")
public interface Bar extends RosettaModelObject {

	BarMeta metaData = new BarMeta();

	/*********************** Getter Methods  ***********************/
	ReferenceWithMetaFoo getB();

	/*********************** Build Methods  ***********************/
	Bar build();
	
	Bar.BarBuilder toBuilder();
	
	static Bar.BarBuilder builder() {
		return new Bar.BarBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Bar> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Bar> getType() {
		return Bar.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("b"), processor, ReferenceWithMetaFoo.class, getB());
	}
	

	/*********************** Builder Interface  ***********************/
	interface BarBuilder extends Bar, RosettaModelObjectBuilder {
		ReferenceWithMetaFoo.ReferenceWithMetaFooBuilder getOrCreateB();
		@Override
		ReferenceWithMetaFoo.ReferenceWithMetaFooBuilder getB();
		Bar.BarBuilder setB(ReferenceWithMetaFoo b);
		Bar.BarBuilder setBValue(Foo b);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("b"), processor, ReferenceWithMetaFoo.ReferenceWithMetaFooBuilder.class, getB());
		}
		

		Bar.BarBuilder prune();
	}

	/*********************** Immutable Implementation of Bar  ***********************/
	class BarImpl implements Bar {
		private final ReferenceWithMetaFoo b;
		
		protected BarImpl(Bar.BarBuilder builder) {
			this.b = ofNullable(builder.getB()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("b")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("b")
		public ReferenceWithMetaFoo getB() {
			return b;
		}
		
		@Override
		public Bar build() {
			return this;
		}
		
		@Override
		public Bar.BarBuilder toBuilder() {
			Bar.BarBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Bar.BarBuilder builder) {
			ofNullable(getB()).ifPresent(builder::setB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Bar _that = getType().cast(o);
		
			if (!Objects.equals(b, _that.getB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (b != null ? b.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Bar {" +
				"b=" + this.b +
			'}';
		}
	}

	/*********************** Builder Implementation of Bar  ***********************/
	class BarBuilderImpl implements Bar.BarBuilder {
	
		protected ReferenceWithMetaFoo.ReferenceWithMetaFooBuilder b;
		
		@Override
		@RosettaAttribute("b")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("b")
		public ReferenceWithMetaFoo.ReferenceWithMetaFooBuilder getB() {
			return b;
		}
		
		@Override
		public ReferenceWithMetaFoo.ReferenceWithMetaFooBuilder getOrCreateB() {
			ReferenceWithMetaFoo.ReferenceWithMetaFooBuilder result;
			if (b!=null) {
				result = b;
			}
			else {
				result = b = ReferenceWithMetaFoo.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("b")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("b")
		@Override
		public Bar.BarBuilder setB(ReferenceWithMetaFoo _b) {
			this.b = _b == null ? null : _b.toBuilder();
			return this;
		}
		
		@Override
		public Bar.BarBuilder setBValue(Foo _b) {
			this.getOrCreateB().setValue(_b);
			return this;
		}
		
		@Override
		public Bar build() {
			return new Bar.BarImpl(this);
		}
		
		@Override
		public Bar.BarBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Bar.BarBuilder prune() {
			if (b!=null && !b.prune().hasData()) b = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getB()!=null && getB().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Bar.BarBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Bar.BarBuilder o = (Bar.BarBuilder) other;
			
			merger.mergeRosetta(getB(), o.getB(), this::setB);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Bar _that = getType().cast(o);
		
			if (!Objects.equals(b, _that.getB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (b != null ? b.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BarBuilder {" +
				"b=" + this.b +
			'}';
		}
	}
}
