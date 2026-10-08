package chaos.s26.a2dangle.unused;

import chaos.s26.a2dangle.unused.meta.C26TagUnusedTMeta;
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
@RosettaDataType(value="C26TagUnusedT", builder=C26TagUnusedT.C26TagUnusedTBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C26TagUnusedT", model="chaos", builder=C26TagUnusedT.C26TagUnusedTBuilderImpl.class, version="1.0.0")
public interface C26TagUnusedT extends RosettaModelObject {

	C26TagUnusedTMeta metaData = new C26TagUnusedTMeta();

	/*********************** Getter Methods  ***********************/
	String getStub();

	/*********************** Build Methods  ***********************/
	C26TagUnusedT build();
	
	C26TagUnusedT.C26TagUnusedTBuilder toBuilder();
	
	static C26TagUnusedT.C26TagUnusedTBuilder builder() {
		return new C26TagUnusedT.C26TagUnusedTBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C26TagUnusedT> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C26TagUnusedT> getType() {
		return C26TagUnusedT.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C26TagUnusedTBuilder extends C26TagUnusedT, RosettaModelObjectBuilder {
		C26TagUnusedT.C26TagUnusedTBuilder setStub(String stub);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stub"), String.class, getStub(), this);
		}
		

		C26TagUnusedT.C26TagUnusedTBuilder prune();
	}

	/*********************** Immutable Implementation of C26TagUnusedT  ***********************/
	class C26TagUnusedTImpl implements C26TagUnusedT {
		private final String stub;
		
		protected C26TagUnusedTImpl(C26TagUnusedT.C26TagUnusedTBuilder builder) {
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
		public C26TagUnusedT build() {
			return this;
		}
		
		@Override
		public C26TagUnusedT.C26TagUnusedTBuilder toBuilder() {
			C26TagUnusedT.C26TagUnusedTBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C26TagUnusedT.C26TagUnusedTBuilder builder) {
			ofNullable(getStub()).ifPresent(builder::setStub);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C26TagUnusedT _that = getType().cast(o);
		
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
			return "C26TagUnusedT {" +
				"stub=" + this.stub +
			'}';
		}
	}

	/*********************** Builder Implementation of C26TagUnusedT  ***********************/
	class C26TagUnusedTBuilderImpl implements C26TagUnusedT.C26TagUnusedTBuilder {
	
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
		public C26TagUnusedT.C26TagUnusedTBuilder setStub(String _stub) {
			this.stub = _stub == null ? null : _stub;
			return this;
		}
		
		@Override
		public C26TagUnusedT build() {
			return new C26TagUnusedT.C26TagUnusedTImpl(this);
		}
		
		@Override
		public C26TagUnusedT.C26TagUnusedTBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C26TagUnusedT.C26TagUnusedTBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStub()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C26TagUnusedT.C26TagUnusedTBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C26TagUnusedT.C26TagUnusedTBuilder o = (C26TagUnusedT.C26TagUnusedTBuilder) other;
			
			
			merger.mergeBasic(getStub(), o.getStub(), this::setStub);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C26TagUnusedT _that = getType().cast(o);
		
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
			return "C26TagUnusedTBuilder {" +
				"stub=" + this.stub +
			'}';
		}
	}
}
