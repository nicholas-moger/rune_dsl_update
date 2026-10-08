package chaos.s05.a2dangle.unused;

import chaos.s05.a2dangle.unused.meta.C5SubUnusedTMeta;
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
@RosettaDataType(value="C5SubUnusedT", builder=C5SubUnusedT.C5SubUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C5SubUnusedT", model="chaos", builder=C5SubUnusedT.C5SubUnusedTBuilderImpl.class, version="1.0.0")
public interface C5SubUnusedT extends RosettaModelObject {

	C5SubUnusedTMeta metaData = new C5SubUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C5SubUnusedT build();
	
	C5SubUnusedT.C5SubUnusedTBuilder toBuilder();
	
	static C5SubUnusedT.C5SubUnusedTBuilder builder() {
		return new C5SubUnusedT.C5SubUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C5SubUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C5SubUnusedT> getType() {
		return C5SubUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C5SubUnusedTBuilder extends C5SubUnusedT, RosettaModelObjectBuilder {
		C5SubUnusedT.C5SubUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C5SubUnusedT.C5SubUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C5SubUnusedT  ***********************/
	class C5SubUnusedTImpl implements C5SubUnusedT {
		private final String stub;
		
		protected C5SubUnusedTImpl(C5SubUnusedT.C5SubUnusedTBuilder builder) {
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
		public C5SubUnusedT build() {
			return this;
		}
		
		@Override
		public C5SubUnusedT.C5SubUnusedTBuilder toBuilder() {
			C5SubUnusedT.C5SubUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C5SubUnusedT.C5SubUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C5SubUnusedT _that = getType().cast(o);
		
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
			return "C5SubUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C5SubUnusedT  ***********************/
	class C5SubUnusedTBuilderImpl implements C5SubUnusedT.C5SubUnusedTBuilder {
	
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
		public C5SubUnusedT.C5SubUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C5SubUnusedT build() {
			return new C5SubUnusedT.C5SubUnusedTImpl(this);
		}
		
		@Override
		public C5SubUnusedT.C5SubUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C5SubUnusedT.C5SubUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C5SubUnusedT.C5SubUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C5SubUnusedT.C5SubUnusedTBuilder o = (C5SubUnusedT.C5SubUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C5SubUnusedT _that = getType().cast(o);
		
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
			return "C5SubUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
