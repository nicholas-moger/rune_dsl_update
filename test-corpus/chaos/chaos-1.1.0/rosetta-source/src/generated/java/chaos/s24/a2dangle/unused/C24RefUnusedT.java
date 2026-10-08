package chaos.s24.a2dangle.unused;

import chaos.s24.a2dangle.unused.meta.C24RefUnusedTMeta;
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
 * @version 1.0.0
 */
@RosettaDataType(value="C24RefUnusedT", builder=C24RefUnusedT.C24RefUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C24RefUnusedT", model="chaos", builder=C24RefUnusedT.C24RefUnusedTBuilderImpl.class, version="1.0.0")
public interface C24RefUnusedT extends RosettaModelObject {

	C24RefUnusedTMeta metaData = new C24RefUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C24RefUnusedT build();
	
	C24RefUnusedT.C24RefUnusedTBuilder toBuilder();
	
	static C24RefUnusedT.C24RefUnusedTBuilder builder() {
		return new C24RefUnusedT.C24RefUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C24RefUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C24RefUnusedT> getType() {
		return C24RefUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C24RefUnusedTBuilder extends C24RefUnusedT, RosettaModelObjectBuilder {
		C24RefUnusedT.C24RefUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C24RefUnusedT.C24RefUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C24RefUnusedT  ***********************/
	class C24RefUnusedTImpl implements C24RefUnusedT {
		private final String stub;
		
		protected C24RefUnusedTImpl(C24RefUnusedT.C24RefUnusedTBuilder builder) {
			this.stub = builder.getStub();
		}
		
		@Override
		@RosettaAttribute("stub")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("stub")
		public String getStub() {
			return stub;
		}
		
		@Override
		public C24RefUnusedT build() {
			return this;
		}
		
		@Override
		public C24RefUnusedT.C24RefUnusedTBuilder toBuilder() {
			C24RefUnusedT.C24RefUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C24RefUnusedT.C24RefUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C24RefUnusedT _that = getType().cast(o);
		
			if (!Objects.equals(stub, _that.getStub())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (stub != null ? stub.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C24RefUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C24RefUnusedT  ***********************/
	class C24RefUnusedTBuilderImpl implements C24RefUnusedT.C24RefUnusedTBuilder {
	
		protected String stub;
		
		@Override
		@RosettaAttribute("stub")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("stub")
		public String getStub() {
			return stub;
		}
		
		@RosettaAttribute("stub")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("stub")
		@Override
		public C24RefUnusedT.C24RefUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C24RefUnusedT build() {
			return new C24RefUnusedT.C24RefUnusedTImpl(this);
		}
		
		@Override
		public C24RefUnusedT.C24RefUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C24RefUnusedT.C24RefUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C24RefUnusedT.C24RefUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C24RefUnusedT.C24RefUnusedTBuilder o = (C24RefUnusedT.C24RefUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C24RefUnusedT _that = getType().cast(o);
		
			if (!Objects.equals(stub, _that.getStub())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (stub != null ? stub.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C24RefUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
