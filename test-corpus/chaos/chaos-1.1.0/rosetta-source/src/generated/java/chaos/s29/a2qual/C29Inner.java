package chaos.s29.a2qual;

import chaos.s29.a2qual.meta.C29InnerMeta;
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
 * The inner choice, reached through both outer options.
 * @version 1.0.0
 */
@RosettaDataType(value="C29Inner", builder=C29Inner.C29InnerBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C29Inner", model="chaos", builder=C29Inner.C29InnerBuilderImpl.class, version="1.0.0")
@RuneChoiceType
public interface C29Inner extends RosettaModelObject {

	C29InnerMeta metaData = new C29InnerMeta();

	/*********************** Getter Methods  ***********************/
	C29In1 getC29In1();
	C29In2 getC29In2();

	/*********************** Build Methods  ***********************/
	C29Inner build();
	
	C29Inner.C29InnerBuilder toBuilder();
	
	static C29Inner.C29InnerBuilder builder() {
		return new C29Inner.C29InnerBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C29Inner> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C29Inner> getType() {
		return C29Inner.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("C29In1"), processor, C29In1.class, getC29In1());
		processRosetta(path.newSubPath("C29In2"), processor, C29In2.class, getC29In2());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C29InnerBuilder extends C29Inner, RosettaModelObjectBuilder {
		C29In1.C29In1Builder getOrCreateC29In1();
		@Override
		C29In1.C29In1Builder getC29In1();
		C29In2.C29In2Builder getOrCreateC29In2();
		@Override
		C29In2.C29In2Builder getC29In2();
		C29Inner.C29InnerBuilder setC29In1(C29In1 _C29In1);
		C29Inner.C29InnerBuilder setC29In2(C29In2 _C29In2);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("C29In1"), processor, C29In1.C29In1Builder.class, getC29In1());
			processRosetta(path.newSubPath("C29In2"), processor, C29In2.C29In2Builder.class, getC29In2());
		}
		

		C29Inner.C29InnerBuilder prune();
	}

	/*********************** Immutable Implementation of C29Inner  ***********************/
	class C29InnerImpl implements C29Inner {
		private final C29In1 c29In1;
		private final C29In2 c29In2;
		
		protected C29InnerImpl(C29Inner.C29InnerBuilder builder) {
			this.c29In1 = ofNullable(builder.getC29In1()).map(f->f.build()).orElse(null);
			this.c29In2 = ofNullable(builder.getC29In2()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("C29In1")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C29In1")
		public C29In1 getC29In1() {
			return c29In1;
		}
		
		@Override
		@RosettaAttribute("C29In2")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C29In2")
		public C29In2 getC29In2() {
			return c29In2;
		}
		
		@Override
		public C29Inner build() {
			return this;
		}
		
		@Override
		public C29Inner.C29InnerBuilder toBuilder() {
			C29Inner.C29InnerBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C29Inner.C29InnerBuilder builder) {
			ofNullable(getC29In1()).ifPresent(builder::setC29In1);
			ofNullable(getC29In2()).ifPresent(builder::setC29In2);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C29Inner _that = getType().cast(o);
		
			if (!Objects.equals(c29In1, _that.getC29In1())) return false;
			if (!Objects.equals(c29In2, _that.getC29In2())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c29In1 != null ? c29In1.hashCode() : 0);
			_result = 31 * _result + (c29In2 != null ? c29In2.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C29Inner {" +
				"C29In1=" + this.c29In1 + ", " +
				"C29In2=" + this.c29In2 +
			'}';
		}
	}

	/*********************** Builder Implementation of C29Inner  ***********************/
	class C29InnerBuilderImpl implements C29Inner.C29InnerBuilder {
	
		protected C29In1.C29In1Builder c29In1;
		protected C29In2.C29In2Builder c29In2;
		
		@Override
		@RosettaAttribute("C29In1")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C29In1")
		public C29In1.C29In1Builder getC29In1() {
			return c29In1;
		}
		
		@Override
		public C29In1.C29In1Builder getOrCreateC29In1() {
			C29In1.C29In1Builder result;
			if (c29In1!=null) {
				result = c29In1;
			}
			else {
				result = c29In1 = C29In1.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("C29In2")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C29In2")
		public C29In2.C29In2Builder getC29In2() {
			return c29In2;
		}
		
		@Override
		public C29In2.C29In2Builder getOrCreateC29In2() {
			C29In2.C29In2Builder result;
			if (c29In2!=null) {
				result = c29In2;
			}
			else {
				result = c29In2 = C29In2.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("C29In1")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C29In1")
		@Override
		public C29Inner.C29InnerBuilder setC29In1(C29In1 _c29In1) {
			this.c29In1 = _c29In1 == null ? null : _c29In1.toBuilder();
			return this;
		}
		
		@RosettaAttribute("C29In2")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C29In2")
		@Override
		public C29Inner.C29InnerBuilder setC29In2(C29In2 _c29In2) {
			this.c29In2 = _c29In2 == null ? null : _c29In2.toBuilder();
			return this;
		}
		
		@Override
		public C29Inner build() {
			return new C29Inner.C29InnerImpl(this);
		}
		
		@Override
		public C29Inner.C29InnerBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C29Inner.C29InnerBuilder prune() {
			if (c29In1!=null && !c29In1.prune().hasData()) c29In1 = null;
			if (c29In2!=null && !c29In2.prune().hasData()) c29In2 = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getC29In1()!=null && getC29In1().hasData()) return true;
			if (getC29In2()!=null && getC29In2().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C29Inner.C29InnerBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C29Inner.C29InnerBuilder o = (C29Inner.C29InnerBuilder) other;
			
			merger.mergeRosetta(getC29In1(), o.getC29In1(), this::setC29In1);
			merger.mergeRosetta(getC29In2(), o.getC29In2(), this::setC29In2);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C29Inner _that = getType().cast(o);
		
			if (!Objects.equals(c29In1, _that.getC29In1())) return false;
			if (!Objects.equals(c29In2, _that.getC29In2())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c29In1 != null ? c29In1.hashCode() : 0);
			_result = 31 * _result + (c29In2 != null ? c29In2.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C29InnerBuilder {" +
				"C29In1=" + this.c29In1 + ", " +
				"C29In2=" + this.c29In2 +
			'}';
		}
	}
}
