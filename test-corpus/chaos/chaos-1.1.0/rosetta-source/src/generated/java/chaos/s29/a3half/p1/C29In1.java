package chaos.s29.a3half.p1;

import chaos.s29.a3half.p1.meta.C29In1Meta;
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

import static java.util.Optional.ofNullable;

/**
 * Inner option 1.
 * @version 1.0.0
 */
@RosettaDataType(value="C29In1", builder=C29In1.C29In1BuilderImpl.class, version="1.0.0")
@RuneDataType(value="C29In1", model="chaos", builder=C29In1.C29In1BuilderImpl.class, version="1.0.0")
public interface C29In1 extends RosettaModelObject {

	C29In1Meta metaData = new C29In1Meta();

	/*********************** Getter Methods  ***********************/
	String getDeep();

	/*********************** Build Methods  ***********************/
	C29In1 build();
	
	C29In1.C29In1Builder toBuilder();
	
	static C29In1.C29In1Builder builder() {
		return new C29In1.C29In1BuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C29In1> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C29In1> getType() {
		return C29In1.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("deep"), String.class, getDeep(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C29In1Builder extends C29In1, RosettaModelObjectBuilder {
		C29In1.C29In1Builder setDeep(String deep);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("deep"), String.class, getDeep(), this);
		}
		

		C29In1.C29In1Builder prune();
	}

	/*********************** Immutable Implementation of C29In1  ***********************/
	class C29In1Impl implements C29In1 {
		private final String deep;
		
		protected C29In1Impl(C29In1.C29In1Builder builder) {
			this.deep = builder.getDeep();
		}
		
		@Override
		@RosettaAttribute("deep")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("deep")
		public String getDeep() {
			return deep;
		}
		
		@Override
		public C29In1 build() {
			return this;
		}
		
		@Override
		public C29In1.C29In1Builder toBuilder() {
			C29In1.C29In1Builder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C29In1.C29In1Builder builder) {
			ofNullable(getDeep()).ifPresent(builder::setDeep);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C29In1 _that = getType().cast(o);
		
			if (!Objects.equals(deep, _that.getDeep())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (deep != null ? deep.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C29In1 {" +
				"deep=" + this.deep +
			'}';
		}
	}

	/*********************** Builder Implementation of C29In1  ***********************/
	class C29In1BuilderImpl implements C29In1.C29In1Builder {
	
		protected String deep;
		
		@Override
		@RosettaAttribute("deep")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("deep")
		public String getDeep() {
			return deep;
		}
		
		@RosettaAttribute("deep")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("deep")
		@Override
		public C29In1.C29In1Builder setDeep(String _deep) {
			this.deep = _deep == null ? null : _deep;
			return this;
		}
		
		@Override
		public C29In1 build() {
			return new C29In1.C29In1Impl(this);
		}
		
		@Override
		public C29In1.C29In1Builder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C29In1.C29In1Builder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getDeep()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C29In1.C29In1Builder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C29In1.C29In1Builder o = (C29In1.C29In1Builder) other;
			
			
			merger.mergeBasic(getDeep(), o.getDeep(), this::setDeep);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C29In1 _that = getType().cast(o);
		
			if (!Objects.equals(deep, _that.getDeep())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (deep != null ? deep.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C29In1Builder {" +
				"deep=" + this.deep +
			'}';
		}
	}
}
