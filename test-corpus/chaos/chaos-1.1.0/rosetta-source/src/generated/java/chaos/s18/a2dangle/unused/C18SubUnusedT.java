package chaos.s18.a2dangle.unused;

import chaos.s18.a2dangle.unused.meta.C18SubUnusedTMeta;
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
@RosettaDataType(value="C18SubUnusedT", builder=C18SubUnusedT.C18SubUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C18SubUnusedT", model="chaos", builder=C18SubUnusedT.C18SubUnusedTBuilderImpl.class, version="1.0.0")
public interface C18SubUnusedT extends RosettaModelObject {

	C18SubUnusedTMeta metaData = new C18SubUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C18SubUnusedT build();
	
	C18SubUnusedT.C18SubUnusedTBuilder toBuilder();
	
	static C18SubUnusedT.C18SubUnusedTBuilder builder() {
		return new C18SubUnusedT.C18SubUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C18SubUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C18SubUnusedT> getType() {
		return C18SubUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C18SubUnusedTBuilder extends C18SubUnusedT, RosettaModelObjectBuilder {
		C18SubUnusedT.C18SubUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C18SubUnusedT.C18SubUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C18SubUnusedT  ***********************/
	class C18SubUnusedTImpl implements C18SubUnusedT {
		private final String stub;
		
		protected C18SubUnusedTImpl(C18SubUnusedT.C18SubUnusedTBuilder builder) {
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
		public C18SubUnusedT build() {
			return this;
		}
		
		@Override
		public C18SubUnusedT.C18SubUnusedTBuilder toBuilder() {
			C18SubUnusedT.C18SubUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C18SubUnusedT.C18SubUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C18SubUnusedT _that = getType().cast(o);
		
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
			return "C18SubUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C18SubUnusedT  ***********************/
	class C18SubUnusedTBuilderImpl implements C18SubUnusedT.C18SubUnusedTBuilder {
	
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
		public C18SubUnusedT.C18SubUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C18SubUnusedT build() {
			return new C18SubUnusedT.C18SubUnusedTImpl(this);
		}
		
		@Override
		public C18SubUnusedT.C18SubUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C18SubUnusedT.C18SubUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C18SubUnusedT.C18SubUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C18SubUnusedT.C18SubUnusedTBuilder o = (C18SubUnusedT.C18SubUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C18SubUnusedT _that = getType().cast(o);
		
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
			return "C18SubUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
