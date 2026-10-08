package chaos.s23.a2dangle.unused;

import chaos.s23.a2dangle.unused.meta.C23BoxUnusedTMeta;
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
@RosettaDataType(value="C23BoxUnusedT", builder=C23BoxUnusedT.C23BoxUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C23BoxUnusedT", model="chaos", builder=C23BoxUnusedT.C23BoxUnusedTBuilderImpl.class, version="1.0.0")
public interface C23BoxUnusedT extends RosettaModelObject {

	C23BoxUnusedTMeta metaData = new C23BoxUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C23BoxUnusedT build();
	
	C23BoxUnusedT.C23BoxUnusedTBuilder toBuilder();
	
	static C23BoxUnusedT.C23BoxUnusedTBuilder builder() {
		return new C23BoxUnusedT.C23BoxUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C23BoxUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C23BoxUnusedT> getType() {
		return C23BoxUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C23BoxUnusedTBuilder extends C23BoxUnusedT, RosettaModelObjectBuilder {
		C23BoxUnusedT.C23BoxUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C23BoxUnusedT.C23BoxUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C23BoxUnusedT  ***********************/
	class C23BoxUnusedTImpl implements C23BoxUnusedT {
		private final String stub;
		
		protected C23BoxUnusedTImpl(C23BoxUnusedT.C23BoxUnusedTBuilder builder) {
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
		public C23BoxUnusedT build() {
			return this;
		}
		
		@Override
		public C23BoxUnusedT.C23BoxUnusedTBuilder toBuilder() {
			C23BoxUnusedT.C23BoxUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C23BoxUnusedT.C23BoxUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C23BoxUnusedT _that = getType().cast(o);
		
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
			return "C23BoxUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C23BoxUnusedT  ***********************/
	class C23BoxUnusedTBuilderImpl implements C23BoxUnusedT.C23BoxUnusedTBuilder {
	
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
		public C23BoxUnusedT.C23BoxUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C23BoxUnusedT build() {
			return new C23BoxUnusedT.C23BoxUnusedTImpl(this);
		}
		
		@Override
		public C23BoxUnusedT.C23BoxUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C23BoxUnusedT.C23BoxUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C23BoxUnusedT.C23BoxUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C23BoxUnusedT.C23BoxUnusedTBuilder o = (C23BoxUnusedT.C23BoxUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C23BoxUnusedT _that = getType().cast(o);
		
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
			return "C23BoxUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
