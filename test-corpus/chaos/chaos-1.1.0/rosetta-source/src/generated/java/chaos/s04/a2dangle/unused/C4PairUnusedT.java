package chaos.s04.a2dangle.unused;

import chaos.s04.a2dangle.unused.meta.C4PairUnusedTMeta;
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
@RosettaDataType(value="C4PairUnusedT", builder=C4PairUnusedT.C4PairUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C4PairUnusedT", model="chaos", builder=C4PairUnusedT.C4PairUnusedTBuilderImpl.class, version="1.0.0")
public interface C4PairUnusedT extends RosettaModelObject {

	C4PairUnusedTMeta metaData = new C4PairUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C4PairUnusedT build();
	
	C4PairUnusedT.C4PairUnusedTBuilder toBuilder();
	
	static C4PairUnusedT.C4PairUnusedTBuilder builder() {
		return new C4PairUnusedT.C4PairUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C4PairUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C4PairUnusedT> getType() {
		return C4PairUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C4PairUnusedTBuilder extends C4PairUnusedT, RosettaModelObjectBuilder {
		C4PairUnusedT.C4PairUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C4PairUnusedT.C4PairUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C4PairUnusedT  ***********************/
	class C4PairUnusedTImpl implements C4PairUnusedT {
		private final String stub;
		
		protected C4PairUnusedTImpl(C4PairUnusedT.C4PairUnusedTBuilder builder) {
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
		public C4PairUnusedT build() {
			return this;
		}
		
		@Override
		public C4PairUnusedT.C4PairUnusedTBuilder toBuilder() {
			C4PairUnusedT.C4PairUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C4PairUnusedT.C4PairUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C4PairUnusedT _that = getType().cast(o);
		
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
			return "C4PairUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C4PairUnusedT  ***********************/
	class C4PairUnusedTBuilderImpl implements C4PairUnusedT.C4PairUnusedTBuilder {
	
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
		public C4PairUnusedT.C4PairUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C4PairUnusedT build() {
			return new C4PairUnusedT.C4PairUnusedTImpl(this);
		}
		
		@Override
		public C4PairUnusedT.C4PairUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C4PairUnusedT.C4PairUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C4PairUnusedT.C4PairUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C4PairUnusedT.C4PairUnusedTBuilder o = (C4PairUnusedT.C4PairUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C4PairUnusedT _that = getType().cast(o);
		
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
			return "C4PairUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
