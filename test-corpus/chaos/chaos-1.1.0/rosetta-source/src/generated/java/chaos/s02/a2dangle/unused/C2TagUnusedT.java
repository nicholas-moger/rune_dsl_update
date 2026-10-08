package chaos.s02.a2dangle.unused;

import chaos.s02.a2dangle.unused.meta.C2TagUnusedTMeta;
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
@RosettaDataType(value="C2TagUnusedT", builder=C2TagUnusedT.C2TagUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C2TagUnusedT", model="chaos", builder=C2TagUnusedT.C2TagUnusedTBuilderImpl.class, version="1.0.0")
public interface C2TagUnusedT extends RosettaModelObject {

	C2TagUnusedTMeta metaData = new C2TagUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C2TagUnusedT build();
	
	C2TagUnusedT.C2TagUnusedTBuilder toBuilder();
	
	static C2TagUnusedT.C2TagUnusedTBuilder builder() {
		return new C2TagUnusedT.C2TagUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C2TagUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C2TagUnusedT> getType() {
		return C2TagUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C2TagUnusedTBuilder extends C2TagUnusedT, RosettaModelObjectBuilder {
		C2TagUnusedT.C2TagUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C2TagUnusedT.C2TagUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C2TagUnusedT  ***********************/
	class C2TagUnusedTImpl implements C2TagUnusedT {
		private final String stub;
		
		protected C2TagUnusedTImpl(C2TagUnusedT.C2TagUnusedTBuilder builder) {
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
		public C2TagUnusedT build() {
			return this;
		}
		
		@Override
		public C2TagUnusedT.C2TagUnusedTBuilder toBuilder() {
			C2TagUnusedT.C2TagUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C2TagUnusedT.C2TagUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C2TagUnusedT _that = getType().cast(o);
		
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
			return "C2TagUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C2TagUnusedT  ***********************/
	class C2TagUnusedTBuilderImpl implements C2TagUnusedT.C2TagUnusedTBuilder {
	
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
		public C2TagUnusedT.C2TagUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C2TagUnusedT build() {
			return new C2TagUnusedT.C2TagUnusedTImpl(this);
		}
		
		@Override
		public C2TagUnusedT.C2TagUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C2TagUnusedT.C2TagUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C2TagUnusedT.C2TagUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C2TagUnusedT.C2TagUnusedTBuilder o = (C2TagUnusedT.C2TagUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C2TagUnusedT _that = getType().cast(o);
		
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
			return "C2TagUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
