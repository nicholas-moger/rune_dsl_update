package chaos.s17.a2dangle.unused;

import chaos.s17.a2dangle.unused.meta.C17HeldUnusedTMeta;
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
@RosettaDataType(value="C17HeldUnusedT", builder=C17HeldUnusedT.C17HeldUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C17HeldUnusedT", model="chaos", builder=C17HeldUnusedT.C17HeldUnusedTBuilderImpl.class, version="1.0.0")
public interface C17HeldUnusedT extends RosettaModelObject {

	C17HeldUnusedTMeta metaData = new C17HeldUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C17HeldUnusedT build();
	
	C17HeldUnusedT.C17HeldUnusedTBuilder toBuilder();
	
	static C17HeldUnusedT.C17HeldUnusedTBuilder builder() {
		return new C17HeldUnusedT.C17HeldUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C17HeldUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C17HeldUnusedT> getType() {
		return C17HeldUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C17HeldUnusedTBuilder extends C17HeldUnusedT, RosettaModelObjectBuilder {
		C17HeldUnusedT.C17HeldUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C17HeldUnusedT.C17HeldUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C17HeldUnusedT  ***********************/
	class C17HeldUnusedTImpl implements C17HeldUnusedT {
		private final String stub;
		
		protected C17HeldUnusedTImpl(C17HeldUnusedT.C17HeldUnusedTBuilder builder) {
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
		public C17HeldUnusedT build() {
			return this;
		}
		
		@Override
		public C17HeldUnusedT.C17HeldUnusedTBuilder toBuilder() {
			C17HeldUnusedT.C17HeldUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C17HeldUnusedT.C17HeldUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C17HeldUnusedT _that = getType().cast(o);
		
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
			return "C17HeldUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C17HeldUnusedT  ***********************/
	class C17HeldUnusedTBuilderImpl implements C17HeldUnusedT.C17HeldUnusedTBuilder {
	
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
		public C17HeldUnusedT.C17HeldUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C17HeldUnusedT build() {
			return new C17HeldUnusedT.C17HeldUnusedTImpl(this);
		}
		
		@Override
		public C17HeldUnusedT.C17HeldUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C17HeldUnusedT.C17HeldUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C17HeldUnusedT.C17HeldUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C17HeldUnusedT.C17HeldUnusedTBuilder o = (C17HeldUnusedT.C17HeldUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C17HeldUnusedT _that = getType().cast(o);
		
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
			return "C17HeldUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
