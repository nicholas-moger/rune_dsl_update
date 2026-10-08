package chaos.s29.a5mixed;

import chaos.s29.a5mixed.meta.C29In2Meta;
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
 * Inner option 2.
 * @version 1.0.0
 */
@RosettaDataType(value="C29In2", builder=C29In2.C29In2BuilderImpl.class, version="1.0.0")
@RuneDataType(value="C29In2", model="chaos", builder=C29In2.C29In2BuilderImpl.class, version="1.0.0")
public interface C29In2 extends RosettaModelObject {

	C29In2Meta metaData = new C29In2Meta();

	/*********************** Getter Methods  ***********************/
	String getDeep();

	/*********************** Build Methods  ***********************/
	C29In2 build();
	
	C29In2.C29In2Builder toBuilder();
	
	static C29In2.C29In2Builder builder() {
		return new C29In2.C29In2BuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C29In2> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C29In2> getType() {
		return C29In2.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("deep"), String.class, getDeep(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C29In2Builder extends C29In2, RosettaModelObjectBuilder {
		C29In2.C29In2Builder setDeep(String deep);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("deep"), String.class, getDeep(), this);
		}
		

		C29In2.C29In2Builder prune();
	}

	/*********************** Immutable Implementation of C29In2  ***********************/
	class C29In2Impl implements C29In2 {
		private final String deep;
		
		protected C29In2Impl(C29In2.C29In2Builder builder) {
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
		public C29In2 build() {
			return this;
		}
		
		@Override
		public C29In2.C29In2Builder toBuilder() {
			C29In2.C29In2Builder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C29In2.C29In2Builder builder) {
			ofNullable(getDeep()).ifPresent(builder::setDeep);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C29In2 _that = getType().cast(o);
		
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
			return "C29In2 {" +
				"deep=" + this.deep +
			'}';
		}
	}

	/*********************** Builder Implementation of C29In2  ***********************/
	class C29In2BuilderImpl implements C29In2.C29In2Builder {
	
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
		public C29In2.C29In2Builder setDeep(String _deep) {
			this.deep = _deep == null ? null : _deep;
			return this;
		}
		
		@Override
		public C29In2 build() {
			return new C29In2.C29In2Impl(this);
		}
		
		@Override
		public C29In2.C29In2Builder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C29In2.C29In2Builder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getDeep()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C29In2.C29In2Builder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C29In2.C29In2Builder o = (C29In2.C29In2Builder) other;
			
			
			merger.mergeBasic(getDeep(), o.getDeep(), this::setDeep);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C29In2 _that = getType().cast(o);
		
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
			return "C29In2Builder {" +
				"deep=" + this.deep +
			'}';
		}
	}
}
