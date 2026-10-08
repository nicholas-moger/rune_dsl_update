package chaos.s06.a2dangle.unused;

import chaos.s06.a2dangle.unused.meta.C6TagUnusedTMeta;
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
@RosettaDataType(value="C6TagUnusedT", builder=C6TagUnusedT.C6TagUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C6TagUnusedT", model="chaos", builder=C6TagUnusedT.C6TagUnusedTBuilderImpl.class, version="1.0.0")
public interface C6TagUnusedT extends RosettaModelObject {

	C6TagUnusedTMeta metaData = new C6TagUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C6TagUnusedT build();
	
	C6TagUnusedT.C6TagUnusedTBuilder toBuilder();
	
	static C6TagUnusedT.C6TagUnusedTBuilder builder() {
		return new C6TagUnusedT.C6TagUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C6TagUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C6TagUnusedT> getType() {
		return C6TagUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C6TagUnusedTBuilder extends C6TagUnusedT, RosettaModelObjectBuilder {
		C6TagUnusedT.C6TagUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C6TagUnusedT.C6TagUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C6TagUnusedT  ***********************/
	class C6TagUnusedTImpl implements C6TagUnusedT {
		private final String stub;
		
		protected C6TagUnusedTImpl(C6TagUnusedT.C6TagUnusedTBuilder builder) {
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
		public C6TagUnusedT build() {
			return this;
		}
		
		@Override
		public C6TagUnusedT.C6TagUnusedTBuilder toBuilder() {
			C6TagUnusedT.C6TagUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C6TagUnusedT.C6TagUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C6TagUnusedT _that = getType().cast(o);
		
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
			return "C6TagUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C6TagUnusedT  ***********************/
	class C6TagUnusedTBuilderImpl implements C6TagUnusedT.C6TagUnusedTBuilder {
	
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
		public C6TagUnusedT.C6TagUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C6TagUnusedT build() {
			return new C6TagUnusedT.C6TagUnusedTImpl(this);
		}
		
		@Override
		public C6TagUnusedT.C6TagUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C6TagUnusedT.C6TagUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C6TagUnusedT.C6TagUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C6TagUnusedT.C6TagUnusedTBuilder o = (C6TagUnusedT.C6TagUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C6TagUnusedT _that = getType().cast(o);
		
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
			return "C6TagUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
