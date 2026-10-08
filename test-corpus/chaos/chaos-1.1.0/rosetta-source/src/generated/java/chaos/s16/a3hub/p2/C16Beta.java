package chaos.s16.a3hub.p2;

import chaos.s16.a3hub.p2.meta.C16BetaMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
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
 * Choice option 2.
 * @version 1.0.0
 */
@RosettaDataType(value="C16Beta", builder=C16Beta.C16BetaBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C16Beta", model="chaos", builder=C16Beta.C16BetaBuilderImpl.class, version="1.0.0")
public interface C16Beta extends RosettaModelObject {

	C16BetaMeta metaData = new C16BetaMeta();

	/*********************** Getter Methods  ***********************/
	String getB();

	/*********************** Build Methods  ***********************/
	C16Beta build();
	
	C16Beta.C16BetaBuilder toBuilder();
	
	static C16Beta.C16BetaBuilder builder() {
		return new C16Beta.C16BetaBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C16Beta> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C16Beta> getType() {
		return C16Beta.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("b"), String.class, getB(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C16BetaBuilder extends C16Beta, RosettaModelObjectBuilder {
		C16Beta.C16BetaBuilder setB(String b);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("b"), String.class, getB(), this);
		}
		

		C16Beta.C16BetaBuilder prune();
	}

	/*********************** Immutable Implementation of C16Beta  ***********************/
	class C16BetaImpl implements C16Beta {
		private final String b;
		
		protected C16BetaImpl(C16Beta.C16BetaBuilder builder) {
			this.b = builder.getB();
		}
		
		@Override
		@RosettaAttribute("b")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("b")
		public String getB() {
			return b;
		}
		
		@Override
		public C16Beta build() {
			return this;
		}
		
		@Override
		public C16Beta.C16BetaBuilder toBuilder() {
			C16Beta.C16BetaBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C16Beta.C16BetaBuilder builder) {
			ofNullable(getB()).ifPresent(builder::setB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16Beta _that = getType().cast(o);
		
			if (!Objects.equals(b, _that.getB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (b != null ? b.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C16Beta {" +
				"b=" + this.b +
			'}';
		}
	}

	/*********************** Builder Implementation of C16Beta  ***********************/
	class C16BetaBuilderImpl implements C16Beta.C16BetaBuilder {
	
		protected String b;
		
		@Override
		@RosettaAttribute("b")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("b")
		public String getB() {
			return b;
		}
		
		@RosettaAttribute("b")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("b")
		@Override
		public C16Beta.C16BetaBuilder setB(String _b) {
			this.b = _b == null ? null : _b;
			return this;
		}
		
		@Override
		public C16Beta build() {
			return new C16Beta.C16BetaImpl(this);
		}
		
		@Override
		public C16Beta.C16BetaBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16Beta.C16BetaBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getB()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16Beta.C16BetaBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C16Beta.C16BetaBuilder o = (C16Beta.C16BetaBuilder) other;
			
			
			merger.mergeBasic(getB(), o.getB(), this::setB);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16Beta _that = getType().cast(o);
		
			if (!Objects.equals(b, _that.getB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (b != null ? b.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C16BetaBuilder {" +
				"b=" + this.b +
			'}';
		}
	}
}
