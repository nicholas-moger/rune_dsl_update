package chaos.s11.a2dangle.unused;

import chaos.s11.a2dangle.unused.meta.C11AuxUnusedTMeta;
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
@RosettaDataType(value="C11AuxUnusedT", builder=C11AuxUnusedT.C11AuxUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C11AuxUnusedT", model="chaos", builder=C11AuxUnusedT.C11AuxUnusedTBuilderImpl.class, version="1.0.0")
public interface C11AuxUnusedT extends RosettaModelObject {

	C11AuxUnusedTMeta metaData = new C11AuxUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C11AuxUnusedT build();
	
	C11AuxUnusedT.C11AuxUnusedTBuilder toBuilder();
	
	static C11AuxUnusedT.C11AuxUnusedTBuilder builder() {
		return new C11AuxUnusedT.C11AuxUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C11AuxUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C11AuxUnusedT> getType() {
		return C11AuxUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C11AuxUnusedTBuilder extends C11AuxUnusedT, RosettaModelObjectBuilder {
		C11AuxUnusedT.C11AuxUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C11AuxUnusedT.C11AuxUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C11AuxUnusedT  ***********************/
	class C11AuxUnusedTImpl implements C11AuxUnusedT {
		private final String stub;
		
		protected C11AuxUnusedTImpl(C11AuxUnusedT.C11AuxUnusedTBuilder builder) {
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
		public C11AuxUnusedT build() {
			return this;
		}
		
		@Override
		public C11AuxUnusedT.C11AuxUnusedTBuilder toBuilder() {
			C11AuxUnusedT.C11AuxUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C11AuxUnusedT.C11AuxUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C11AuxUnusedT _that = getType().cast(o);
		
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
			return "C11AuxUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C11AuxUnusedT  ***********************/
	class C11AuxUnusedTBuilderImpl implements C11AuxUnusedT.C11AuxUnusedTBuilder {
	
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
		public C11AuxUnusedT.C11AuxUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C11AuxUnusedT build() {
			return new C11AuxUnusedT.C11AuxUnusedTImpl(this);
		}
		
		@Override
		public C11AuxUnusedT.C11AuxUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C11AuxUnusedT.C11AuxUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C11AuxUnusedT.C11AuxUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C11AuxUnusedT.C11AuxUnusedTBuilder o = (C11AuxUnusedT.C11AuxUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C11AuxUnusedT _that = getType().cast(o);
		
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
			return "C11AuxUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
