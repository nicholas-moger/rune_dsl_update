package chaos.s01.a2dangle.unused;

import chaos.s01.a2dangle.unused.meta.C1RefUnusedTMeta;
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
@RosettaDataType(value="C1RefUnusedT", builder=C1RefUnusedT.C1RefUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C1RefUnusedT", model="chaos", builder=C1RefUnusedT.C1RefUnusedTBuilderImpl.class, version="1.0.0")
public interface C1RefUnusedT extends RosettaModelObject {

	C1RefUnusedTMeta metaData = new C1RefUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C1RefUnusedT build();
	
	C1RefUnusedT.C1RefUnusedTBuilder toBuilder();
	
	static C1RefUnusedT.C1RefUnusedTBuilder builder() {
		return new C1RefUnusedT.C1RefUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C1RefUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C1RefUnusedT> getType() {
		return C1RefUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C1RefUnusedTBuilder extends C1RefUnusedT, RosettaModelObjectBuilder {
		C1RefUnusedT.C1RefUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C1RefUnusedT.C1RefUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C1RefUnusedT  ***********************/
	class C1RefUnusedTImpl implements C1RefUnusedT {
		private final String stub;
		
		protected C1RefUnusedTImpl(C1RefUnusedT.C1RefUnusedTBuilder builder) {
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
		public C1RefUnusedT build() {
			return this;
		}
		
		@Override
		public C1RefUnusedT.C1RefUnusedTBuilder toBuilder() {
			C1RefUnusedT.C1RefUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C1RefUnusedT.C1RefUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C1RefUnusedT _that = getType().cast(o);
		
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
			return "C1RefUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C1RefUnusedT  ***********************/
	class C1RefUnusedTBuilderImpl implements C1RefUnusedT.C1RefUnusedTBuilder {
	
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
		public C1RefUnusedT.C1RefUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C1RefUnusedT build() {
			return new C1RefUnusedT.C1RefUnusedTImpl(this);
		}
		
		@Override
		public C1RefUnusedT.C1RefUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C1RefUnusedT.C1RefUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C1RefUnusedT.C1RefUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C1RefUnusedT.C1RefUnusedTBuilder o = (C1RefUnusedT.C1RefUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C1RefUnusedT _that = getType().cast(o);
		
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
			return "C1RefUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
