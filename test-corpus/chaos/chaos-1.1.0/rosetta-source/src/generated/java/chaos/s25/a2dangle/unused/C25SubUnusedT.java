package chaos.s25.a2dangle.unused;

import chaos.s25.a2dangle.unused.meta.C25SubUnusedTMeta;
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
@RosettaDataType(value="C25SubUnusedT", builder=C25SubUnusedT.C25SubUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C25SubUnusedT", model="chaos", builder=C25SubUnusedT.C25SubUnusedTBuilderImpl.class, version="1.0.0")
public interface C25SubUnusedT extends RosettaModelObject {

	C25SubUnusedTMeta metaData = new C25SubUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C25SubUnusedT build();
	
	C25SubUnusedT.C25SubUnusedTBuilder toBuilder();
	
	static C25SubUnusedT.C25SubUnusedTBuilder builder() {
		return new C25SubUnusedT.C25SubUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C25SubUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C25SubUnusedT> getType() {
		return C25SubUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C25SubUnusedTBuilder extends C25SubUnusedT, RosettaModelObjectBuilder {
		C25SubUnusedT.C25SubUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C25SubUnusedT.C25SubUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C25SubUnusedT  ***********************/
	class C25SubUnusedTImpl implements C25SubUnusedT {
		private final String stub;
		
		protected C25SubUnusedTImpl(C25SubUnusedT.C25SubUnusedTBuilder builder) {
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
		public C25SubUnusedT build() {
			return this;
		}
		
		@Override
		public C25SubUnusedT.C25SubUnusedTBuilder toBuilder() {
			C25SubUnusedT.C25SubUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C25SubUnusedT.C25SubUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C25SubUnusedT _that = getType().cast(o);
		
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
			return "C25SubUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C25SubUnusedT  ***********************/
	class C25SubUnusedTBuilderImpl implements C25SubUnusedT.C25SubUnusedTBuilder {
	
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
		public C25SubUnusedT.C25SubUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C25SubUnusedT build() {
			return new C25SubUnusedT.C25SubUnusedTImpl(this);
		}
		
		@Override
		public C25SubUnusedT.C25SubUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C25SubUnusedT.C25SubUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C25SubUnusedT.C25SubUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C25SubUnusedT.C25SubUnusedTBuilder o = (C25SubUnusedT.C25SubUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C25SubUnusedT _that = getType().cast(o);
		
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
			return "C25SubUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
